package com.insipidknight.toarrive.payment.controller;

import com.insipidknight.toarrive.api.dto.PaymentRequest;
import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.payment.service.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payment")
@RequiredArgsConstructor
public class PaymentInternalController {

    private final PaymentGateway paymentGateway;

    @PostMapping("/pay")
    Result<Void> pay(@RequestBody PaymentRequest request) {
        paymentGateway.pay(request.payerId(), request.orderNo(), request.amount());
        return Result.success(null);
    }
}
