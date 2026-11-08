package com.insipidknight.toarrive.user.dto;

import com.insipidknight.toarrive.user.entity.enums.Gender;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileDTO {

    @Size(max = 32, message = "昵称长度不能超过32位")
    private String nickname;

    @Size(max = 255, message = "头像地址长度不能超过255位")
    private String avatarUrl;

    private Gender gender;
}
