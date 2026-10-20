package com.insipidknight.toarrive.api.client;

import com.insipidknight.toarrive.api.dto.PaymentRequest;
import com.insipidknight.toarrive.api.dto.PaymentStatusDTO;
import com.insipidknight.toarrive.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/internal/payment")
public interface PaymentClient {

    @PostMapping("/pay")
    Result<Void> pay(@RequestBody PaymentRequest request);

    /**
     * 查询支付结算结果，不存在时 data 为 null。
     */
    @GetMapping("/{orderNo}/status")
    Result<PaymentStatusDTO> status(@PathVariable String orderNo);
}
