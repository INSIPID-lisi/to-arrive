package com.insipidknight.toarrive.push;

import com.insipidknight.toarrive.api.dto.WsMessage;
import com.insipidknight.toarrive.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/push")
@RequiredArgsConstructor
public class PushInternalController {

    private final PushService pushService;

    @PostMapping("/user/{userId}")
    Result<Void> sendToUser(@PathVariable("userId") Long userId, @RequestBody WsMessage message) {
        pushService.sendToUser(userId, message);
        return Result.success(null);
    }
}
