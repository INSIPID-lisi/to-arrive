package com.insipidknight.toarrive.api.client;

import com.insipidknight.toarrive.api.dto.WsMessage;
import com.insipidknight.toarrive.common.Result;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/internal/push")
public interface PushClient {

    @PostMapping("/user/{userId}")
    Result<Void> sendToUser(@PathVariable("userId") Long userId, @RequestBody WsMessage message);
}

