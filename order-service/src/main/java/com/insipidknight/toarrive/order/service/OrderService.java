package com.insipidknight.toarrive.order.service;

import com.insipidknight.toarrive.api.client.LocationClient;
import com.insipidknight.toarrive.api.client.PushClient;
import com.insipidknight.toarrive.api.client.UserClient;
import com.insipidknight.toarrive.api.dto.NearbyDriverDTO;
import com.insipidknight.toarrive.api.dto.UserBriefDTO;
import com.insipidknight.toarrive.api.dto.WsMessage;
import com.insipidknight.toarrive.api.util.ClientResults;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.common.util.CalculateUtil;
import com.insipidknight.toarrive.common.util.GenerateUtil;
import com.insipidknight.toarrive.order.dto.OrderCreateDTO;
import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.CancelBy;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.mapper.OrderMapper;
import com.insipidknight.toarrive.order.pricing.FareResult;
import com.insipidknight.toarrive.order.pricing.PricingService;
import com.insipidknight.toarrive.order.vo.DispatchOrderVO;
import com.insipidknight.toarrive.order.vo.OrderCreateVO;
import com.insipidknight.toarrive.order.vo.OrderVO;
import com.insipidknight.toarrive.order.vo.PendingOrderVO;
import com.insipidknight.toarrive.common.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final double METERS_PER_KM = 1000d;
    private static final double DEGREES_PER_KM = 111.0d;
    private static final int TIMEOUT_BATCH_LIMIT = 500;

    private final OrderMapper orderMapper;
    private final UserClient userClient;
    private final PricingService pricingService;
    private final PushClient pushClient;
    private final LocationClient locationClient;

    @Value("${app.dispatch.radius-km:3}")
    private double dispatchRadiusKm;

    @Value("${app.dispatch.max-drivers:10}")
    private int dispatchMaxDrivers;

    public OrderCreateVO createOrder(Long userId, OrderCreateDTO dto) {

        if (orderMapper.countActiveByPassenger(userId) > 0) {
            throw new BusinessException("您有进行中的订单，无法重复下单");
        }
        Order order = new Order();
        order.setOrderNo(GenerateUtil.generateOrderNo());
        order.setPassengerId(userId);

        order.setStartAddress(dto.getStartAddress());
        order.setStartLng(dto.getStartLng());
        order.setStartLat(dto.getStartLat());
        order.setEndAddress(dto.getEndAddress());
        order.setEndLng(dto.getEndLng());
        order.setEndLat(dto.getEndLat());

        order.setStatus(OrderStatus.PENDING);
        order.setEstimatePrice(pricingService.estimate(
                dto.getStartLng(), dto.getStartLat(), dto.getEndLng(), dto.getEndLat()).price());

        orderMapper.insert(order);
        dispatch(order);

        return new OrderCreateVO(order);
    }

    /**
     * 广播派单：向起点附近的在线司机推送订单，排除已有进行中订单的司机。
     * 派单失败只记日志，不影响下单结果，订单仍可在抢单池中被主动拉取。
     */
    private void dispatch(Order order) {
        try {
            List<NearbyDriverDTO> candidates = ClientResults.unwrap(locationClient.nearbyDrivers(
                    order.getStartLng(), order.getStartLat(), dispatchRadiusKm, dispatchMaxDrivers));
            if (candidates == null || candidates.isEmpty()) {
                return;
            }
            List<Long> driverIds = candidates.stream().map(NearbyDriverDTO::driverId).toList();
            Set<Long> busyDriverIds = new HashSet<>(orderMapper.selectBusyDriverIds(driverIds));

            for (NearbyDriverDTO candidate : candidates) {
                if (busyDriverIds.contains(candidate.driverId())) {
                    continue;
                }
                try {
                    ClientResults.unwrap(pushClient.sendToUser(candidate.driverId(),
                            new WsMessage("ORDER_DISPATCH", order.getOrderNo(), order.getStatus().name(),
                                    new DispatchOrderVO(order, candidate.distanceMeters()))));
                } catch (Exception e) {
                    log.warn("派单推送失败 orderNo={}, driverId={}: {}",
                            order.getOrderNo(), candidate.driverId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("派单失败 orderNo={}: {}", order.getOrderNo(), e.getMessage());
        }
    }

    public OrderVO getByOrderNo(String orderNo, Long userId) {
        Order order = requireOrder(orderNo);

        boolean isPassenger = userId.equals(order.getPassengerId());
        boolean isDriver = userId.equals(order.getDriverId());
        if (!isPassenger && !isDriver) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "您无权查看该订单");
        }

        return toOrderVO(order);
    }

    public OrderVO acceptOrder(String orderNo, Long driverId) {
        ClientResults.unwrap(userClient.requireOnline(driverId));
        if (orderMapper.countActiveByDriver(driverId) > 0) {
            throw new BusinessException("您有进行中的订单，无法接新单");
        }
        Order order = requireOrder(orderNo);
        OrderStatusMachine.assertTransfer(order.getStatus(), OrderStatus.ACCEPTED);
        if (orderMapper.acceptOrder(order.getId(), driverId) == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        Order updated = requireOrder(orderNo);
        return pushAndReturn(updated, updated.getPassengerId(), "ORDER_ACCEPTED");
    }

    public OrderVO arriveOrder(String orderNo, Long driverId) {
        Order order = requireOrder(orderNo);
        requireAssignedDriver(order, driverId);
        OrderStatusMachine.assertTransfer(order.getStatus(), OrderStatus.ARRIVED);
        if (orderMapper.arriveOrder(order.getId(), driverId) == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        Order updated = requireOrder(orderNo);
        return pushAndReturn(updated, updated.getPassengerId(), "ORDER_ARRIVED");
    }

    public OrderVO startOrder(String orderNo, Long driverId) {
        Order order = requireOrder(orderNo);
        requireAssignedDriver(order, driverId);
        OrderStatusMachine.assertTransfer(order.getStatus(), OrderStatus.IN_TRIP);
        if (orderMapper.startOrder(order.getId(), driverId) == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        Order updated = requireOrder(orderNo);
        return pushAndReturn(updated, updated.getPassengerId(), "ORDER_STARTED");
    }

    public OrderVO finishOrder(String orderNo, Long driverId) {
        Order order = requireOrder(orderNo);
        requireAssignedDriver(order, driverId);
        OrderStatusMachine.assertTransfer(order.getStatus(), OrderStatus.FINISHED);
        FareResult fare = pricingService.settle(order, LocalDateTime.now());
        if (orderMapper.finishOrder(order.getId(), driverId,
                fare.distance(), fare.durationMinutes(), fare.price()) == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        Order updated = requireOrder(orderNo);
        return pushAndReturn(updated, updated.getPassengerId(), "ORDER_FINISHED");
    }

    public OrderVO cancelOrder(String orderNo, Long userId, String reason) {
        Order order = requireOrder(orderNo);

        boolean isPassenger = userId.equals(order.getPassengerId());
        boolean isDriver = userId.equals(order.getDriverId());
        if (!isPassenger && !isDriver) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "您无权取消该订单");
        }
        OrderStatusMachine.assertTransfer(order.getStatus(), OrderStatus.CANCELLED);

        CancelBy cancelBy = isPassenger ? CancelBy.PASSENGER : CancelBy.DRIVER;
        if (orderMapper.cancelOrder(order.getId(), userId, cancelBy.getValue(), reason) == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        Order updated = requireOrder(orderNo);
        Long notifyUserId = isPassenger ? updated.getDriverId() : updated.getPassengerId();
        return pushAndReturn(updated, notifyUserId, "ORDER_CANCELLED");
    }

    /**
     * 系统超时取消：取消创建时间早于 cutoff、仍处于待接单的订单，并逐个推送给对应乘客。
     * 逐单原子条件 UPDATE，抢到取消权（影响 1 行）才推送，避免刚被接单却错推订单取消。
     */
    public void cancelTimeoutOrders(LocalDateTime cutoff) {
        int cancelled = 0;
        for (Order order : orderMapper.selectTimeoutPending(cutoff, TIMEOUT_BATCH_LIMIT)) {
            if (orderMapper.cancelBySystem(order.getId()) == 1) {
                Order updated = requireOrder(order.getOrderNo());
                pushAndReturn(updated, updated.getPassengerId(), "ORDER_CANCELLED");
                cancelled++;
            }
        }
        if (cancelled > 0) {
            log.info("系统超时取消 {} 单待接订单, cutoff={}", cancelled, cutoff);
        }
    }

    public List<OrderVO> listMine(Long userId, UserRole role, int limit) {
        List<Order> orders = role == UserRole.DRIVER
                ? orderMapper.selectByDriverId(userId, limit)
                : orderMapper.selectByPassengerId(userId, limit);
        return orders.stream().map(OrderVO::new).toList();
    }

    /**
     * 抢单池：先用经纬度 bounding box 在库里粗筛，再在内存里按精确距离过滤排序。
     * 避免「先 LIMIT 再过滤距离」导致半径内的订单被挤出结果集。
     */
    public List<PendingOrderVO> listPendingNearby(Double lng, Double lat, Double radiusKm, int limit) {
        double radiusMeters = radiusKm * METERS_PER_KM;
        double latDelta = radiusKm / DEGREES_PER_KM;
        double lngDelta = radiusKm / (DEGREES_PER_KM * Math.max(Math.cos(Math.toRadians(lat)), 0.01d));
        int candidateLimit = Math.max(limit * 5, limit);

        return orderMapper.selectPendingNearby(
                        lng - lngDelta, lng + lngDelta, lat - latDelta, lat + latDelta, candidateLimit).stream()
                .map(order -> new PendingOrderVO(order, CalculateUtil.calculateDistance(
                        lng, lat, order.getStartLng(), order.getStartLat()).doubleValue() * METERS_PER_KM))
                .filter(vo -> vo.getPickupDistanceMeters() <= radiusMeters)
                .sorted(Comparator.comparingDouble(PendingOrderVO::getPickupDistanceMeters))
                .limit(limit)
                .toList();
    }

    private Order requireOrder(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private void requireAssignedDriver(Order order, Long driverId) {
        if (!driverId.equals(order.getDriverId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "您不是该订单的司机");
        }
    }

    private OrderVO toOrderVO(Order order) {
        OrderVO vo = new OrderVO(order);
        vo.setDriverInfo(buildDriverBrief(order.getDriverId()));
        return vo;
    }

    /**
     * 组装返回体并顺带向对端推送状态变更；推送失败只记日志，不阻断业务主流程。
     */
    private OrderVO pushAndReturn(Order order, Long notifyUserId, String type) {
        OrderVO vo = toOrderVO(order);
        try {
            ClientResults.unwrap(pushClient.sendToUser(notifyUserId,
                    new WsMessage(type, order.getOrderNo(), order.getStatus().name(), vo)));
        } catch (Exception e) {
            log.warn("订单状态推送失败 orderNo={}, type={}: {}", order.getOrderNo(), type, e.getMessage());
        }
        return vo;
    }

    private UserBriefDTO buildDriverBrief(Long driverId) {
        if (driverId == null) {
            return null;
        }
        return ClientResults.unwrap(userClient.buildBrief(driverId));
    }
}
