package com.insipidknight.toarrive.payment.controller;

import com.insipidknight.toarrive.api.dto.PaymentRequest;
import com.insipidknight.toarrive.api.dto.PaymentStatusDTO;
import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.payment.service.PaymentGateway;
import com.insipidknight.toarrive.payment.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payment")
@RequiredArgsConstructor
public class PaymentInternalController {

    private final PaymentGateway paymentGateway;
    private final SettlementService settlementService;

    @PostMapping("/pay")
    Result<Void> pay(@RequestBody PaymentRequest request) {
        paymentGateway.pay(request);
        return Result.success(null);
    }

    @GetMapping("/{orderNo}/status")
    Result<PaymentStatusDTO> status(@PathVariable String orderNo) {
        return Result.success(settlementService.getStatus(orderNo));
    }
}
