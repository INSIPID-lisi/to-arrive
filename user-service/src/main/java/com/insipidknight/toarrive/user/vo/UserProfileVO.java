package com.insipidknight.user.vo;

import com.insipidknight.user.entity.enums.Gender;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.user.entity.enums.UserStatus;
import lombok.Data;

@Data
public class UserProfileVO {

    private String uid;

    private String nickname;

    private String avatarUrl;

    private Gender gender;

    private String genderDesc;

    private UserRole role;

    private String roleDesc;

    private String maskedPhone;

    private UserStatus status;
}
