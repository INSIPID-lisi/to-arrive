package com.insipidknight.toarrive.order.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.common.util.CalculateUtil;
import com.insipidknight.toarrive.common.util.GenerateUtil;
import com.insipidknight.toarrive.common.ws.PushService;
import com.insipidknight.toarrive.common.ws.WsMessage;
import com.insipidknight.toarrive.order.dto.OrderCreateDTO;
import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.CancelBy;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.mapper.OrderMapper;
import com.insipidknight.toarrive.order.pricing.FareResult;
import com.insipidknight.toarrive.order.pricing.PricingService;
import com.insipidknight.toarrive.order.vo.OrderCreateVO;
import com.insipidknight.toarrive.order.vo.OrderVO;
import com.insipidknight.toarrive.order.vo.PendingOrderVO;
import com.insipidknight.toarrive.user.entity.enums.UserRole;
import com.insipidknight.toarrive.user.service.DriverService;
import com.insipidknight.toarrive.user.service.UserProfileService;
import com.insipidknight.toarrive.user.vo.DriverBriefVO;
import com.insipidknight.toarrive.user.vo.UserBriefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final double METERS_PER_KM = 1000d;
    private static final int TIMEOUT_BATCH_LIMIT = 500;

    private final OrderMapper orderMapper;
    private final UserProfileService userProfileService;
    private final DriverService driverService;
    private final PricingService pricingService;
    private final PushService pushService;

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
        //TODO 派单

        return new OrderCreateVO(order);
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
        driverService.requireOnline(driverId);
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

    public List<PendingOrderVO> listPendingNearby(Double lng, Double lat, Double radiusKm, int limit) {
        double radiusMeters = radiusKm * METERS_PER_KM;
        return orderMapper.selectPending(limit).stream()
                .map(order -> new PendingOrderVO(order, CalculateUtil.calculateDistance(
                        lng, lat, order.getStartLng(), order.getStartLat()).doubleValue() * METERS_PER_KM))
                .filter(vo -> vo.getPickupDistanceMeters() <= radiusMeters)
                .sorted(Comparator.comparingDouble(PendingOrderVO::getPickupDistanceMeters))
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
     * 组装返回体并顺带向对端推送状态变更（对方离线时 PushService 静默跳过）。
     */
    private OrderVO pushAndReturn(Order order, Long notifyUserId, String type) {
        OrderVO vo = toOrderVO(order);
        pushService.sendToUser(notifyUserId, new WsMessage(type, order.getOrderNo(), order.getStatus().name(), vo));
        return vo;
    }

    private DriverBriefVO buildDriverBrief(Long driverId) {
        if (driverId == null) {
            return null;
        }
        UserBriefVO brief = userProfileService.buildBriefByUserId(driverId);
        if (brief == null) {
            return null;
        }
        DriverBriefVO vo = new DriverBriefVO();
        vo.setUid(brief.getUid());
        vo.setDisplayName(brief.getDisplayName());
        vo.setAvatarUrl(brief.getAvatarUrl());
        vo.setMaskedPhone(brief.getMaskedPhone());
        return vo;
    }
}
