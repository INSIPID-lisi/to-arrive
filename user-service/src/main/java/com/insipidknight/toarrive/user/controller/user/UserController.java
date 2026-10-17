package com.insipidknight.toarrive.user.controller.user;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.user.dto.UpdateProfileDTO;
import com.insipidknight.toarrive.user.service.UserProfileService;
import com.insipidknight.toarrive.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        return Result.success(userProfileService.getProfile(UserContext.getUserId()));
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@RequestBody @Valid UpdateProfileDTO updateProfileDTO) {
        Long userId = UserContext.getUserId();
        userProfileService.updateProfile(userId, updateProfileDTO);
        return Result.success(userProfileService.getProfile(userId));
    }
}
