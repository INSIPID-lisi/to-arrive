package com.insipidknight.toarrive.user.vo;

import com.insipidknight.toarrive.user.entity.enums.UserRole;
import lombok.Data;

@Data
public class UserBriefVO {

    private String uid;
    
    private String displayName;

    private String avatarUrl;

    private UserRole role;

    private String maskedPhone;
}
