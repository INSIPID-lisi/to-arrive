package com.insipidknight.toarrive.user.vo;

import com.insipidknight.toarrive.common.util.MaskUtil;
import com.insipidknight.toarrive.user.entity.User;
import com.insipidknight.toarrive.user.entity.UserProfile;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.toarrive.user.entity.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {

    private Long userId;

    private String uid;

    private String maskedPhone;

    private UserRole role;

    private String roleDesc;

    private UserStatus status;

    private String statusDesc;

    private String nickname;

    private String avatarUrl;

    private LocalDateTime createTime;

    public AdminUserVO(User user, UserProfile profile) {
        this.userId = user.getUserId();
        this.uid = user.getUid();
        this.maskedPhone = MaskUtil.maskPhone(user.getPhone());
        this.role = user.getRole();
        this.roleDesc = user.getRole() == null ? null : user.getRole().getDesc();
        this.status = user.getStatus();
        this.statusDesc = user.getStatus() == null ? null : user.getStatus().getDesc();
        this.createTime = user.getCreateTime();
        if (profile != null) {
            this.nickname = profile.getNickname();
            this.avatarUrl = profile.getAvatarUrl();
        }
    }
}
