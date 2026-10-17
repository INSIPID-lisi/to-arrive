package com.insipidknight.toarrive.user.controller.user;

import com.insipidknight.toarrive.api.dto.UserBriefDTO;
import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.user.service.DriverService;
import com.insipidknight.toarrive.user.service.UserProfileService;
import com.insipidknight.toarrive.user.vo.UserBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
public class UserInternalController {

    private final DriverService driverService;
    private final UserProfileService userProfileService;

    /**
     * 接单前置校验：司机资质通过且已出车，否则抛业务异常由调用方还原。
     */
    @GetMapping("/driver/{driverId}/require-online")
    Result<Void> requireOnline(@PathVariable("driverId") Long driverId) {
        driverService.requireOnline(driverId);
        return Result.success(null);
    }

    @GetMapping("/{userId}/brief")
    Result<UserBriefDTO> buildBrief(@PathVariable("userId") Long userId) {
        UserBriefVO brief = userProfileService.buildBriefByUserId(userId);
        return Result.success(brief == null ? null : toDTO(brief));
    }

    private UserBriefDTO toDTO(UserBriefVO vo) {
        return new UserBriefDTO(vo.getUid(), vo.getDisplayName(), vo.getAvatarUrl(), vo.getRole(), vo.getMaskedPhone());
    }
}
