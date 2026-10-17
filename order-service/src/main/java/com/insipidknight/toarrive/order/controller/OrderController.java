package com.insipidknight.toarrive.order.controller;


import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.order.dto.OrderCreateDTO;
import com.insipidknight.toarrive.order.service.OrderService;
import com.insipidknight.toarrive.order.service.PaymentService;
import com.insipidknight.toarrive.order.vo.OrderCreateVO;
import com.insipidknight.toarrive.order.vo.OrderVO;
import com.insipidknight.toarrive.order.vo.PendingOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping
    public Result<OrderCreateVO> createOrder(@RequestBody @Valid OrderCreateDTO orderCreateDTO) {
        Long userId = UserContext.getUserId();
        OrderCreateVO orderVO = orderService.createOrder(userId, orderCreateDTO);
        return Result.success(orderVO);
    }

    @GetMapping("/mine")
    public Result<List<OrderVO>> myOrders(@RequestParam(defaultValue = "20") Integer limit) {
        Long userId = UserContext.getUserId();
        return Result.success(orderService.listMine(userId, UserContext.getRole(), limit));
    }

    @GetMapping("/pending")
    public Result<List<PendingOrderVO>> pendingOrders(@RequestParam Double lng,
                                                      @RequestParam Double lat,
                                                      @RequestParam(defaultValue = "5") Double radiusKm,
                                                      @RequestParam(defaultValue = "20") Integer limit) {
        requireDriver();
        return Result.success(orderService.listPendingNearby(lng, lat, radiusKm, limit));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderVO> getOrderByNo(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        OrderVO vo = orderService.getByOrderNo(orderNo, userId);
        return Result.success(vo);
    }

    @PostMapping("/{orderNo}/accept")
    public Result<OrderVO> accept(@PathVariable String orderNo) {
        requireDriver();
        return Result.success(orderService.acceptOrder(orderNo, UserContext.getUserId()));
    }

    @PostMapping("/{orderNo}/arrive")
    public Result<OrderVO> arrive(@PathVariable String orderNo) {
        requireDriver();
        return Result.success(orderService.arriveOrder(orderNo, UserContext.getUserId()));
    }

    @PostMapping("/{orderNo}/start")
    public Result<OrderVO> start(@PathVariable String orderNo) {
        requireDriver();
        return Result.success(orderService.startOrder(orderNo, UserContext.getUserId()));
    }

    @PostMapping("/{orderNo}/finish")
    public Result<OrderVO> finish(@PathVariable String orderNo) {
        requireDriver();
        return Result.success(orderService.finishOrder(orderNo, UserContext.getUserId()));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<OrderVO> cancel(@PathVariable String orderNo,
                                  @RequestParam(required = false) String reason) {
        return Result.success(orderService.cancelOrder(orderNo, UserContext.getUserId(), reason));
    }

    @PostMapping("/{orderNo}/pay")
    public Result<OrderVO> pay(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        paymentService.payOrder(orderNo, userId);
        return Result.success(orderService.getByOrderNo(orderNo, userId));
    }

    private void requireDriver() {
        if (UserContext.getRole() != UserRole.DRIVER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅司机可执行该操作");
        }
    }
}
