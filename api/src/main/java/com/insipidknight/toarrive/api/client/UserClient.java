package com.insipidknight.toarrive.api.client;

import com.insipidknight.toarrive.api.dto.UserBriefDTO;
import com.insipidknight.toarrive.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/internal/user")
public interface UserClient {

    @GetMapping("/driver/{driverId}/require-online")
    Result<Void> requireOnline(@PathVariable("driverId") Long driverId);

    @GetMapping("/{userId}/brief")
    Result<UserBriefDTO> buildBrief(@PathVariable("userId") Long userId);
}
