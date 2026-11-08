package com.insipidknight.toarrive.user.vo;


import lombok.Data;

@Data
public class LoginVO {

    private String accessToken;

    private Long expiresIn;

    private UserBriefVO user;
}