package com.insipidknight.toarrive.payment.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.insipidknight.toarrive.api.dto.PaymentRequest;
import com.insipidknight.toarrive.api.dto.PaymentStatusDTO;
import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.payment.service.PaymentGateway;
import com.insipidknight.toarrive.payment.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/payment")
@RequiredArgsConstructor
public class PaymentInternalController {

    private final PaymentGateway paymentGateway;
    private final SettlementService settlementService;

    @PostMapping("/pay")
    @SentinelResource(value = "payment:pay", blockHandler = "payBlocked")
    Result<Void> pay(@RequestBody PaymentRequest request) {
        paymentGateway.pay(request);
        return Result.success(null);
    }

    Result<Void> payBlocked(PaymentRequest request, BlockException e) {
        log.warn("支付分账熔断降级 orderNo={}: {}", request.orderNo(), e.getClass().getSimpleName());
        return Result.error(ErrorCode.SERVICE_UNAVAILABLE, "支付服务繁忙，请稍后重试");
    }

    @GetMapping("/{orderNo}/status")
    Result<PaymentStatusDTO> status(@PathVariable String orderNo) {
        return Result.success(settlementService.getStatus(orderNo));
    }
}
