package com.insipidknight.toarrive.user.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.util.MaskUtil;
import com.insipidknight.toarrive.user.dto.UpdateProfileDTO;
import com.insipidknight.toarrive.user.entity.User;
import com.insipidknight.toarrive.user.entity.UserProfile;
import com.insipidknight.toarrive.user.mapper.AuthMapper;
import com.insipidknight.toarrive.user.mapper.UserProfileMapper;
import com.insipidknight.toarrive.user.vo.UserBriefVO;
import com.insipidknight.toarrive.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final AuthMapper authMapper;

    public UserProfile getByUserId(Long userId) {
        return userProfileMapper.selectByUserId(userId);
    }

    public void removeByUserId(Long userId) {
        userProfileMapper.deleteById(userId);
    }

    public void createDefault(Long userId, String uid) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setNickname("用户" + uid);
        userProfileMapper.insert(profile);
    }

    public UserBriefVO buildBrief(User user) {
        UserProfile profile = getByUserId(user.getUserId());

        UserBriefVO vo = new UserBriefVO();
        vo.setUid(user.getUid());
        vo.setRole(user.getRole());
        vo.setMaskedPhone(MaskUtil.maskPhone(user.getPhone()));
        if (profile != null) {
            vo.setDisplayName(profile.getNickname());
            vo.setAvatarUrl(profile.getAvatarUrl());
        }
        return vo;
    }

    public UserBriefVO buildBriefByUserId(Long userId) {
        User user = authMapper.selectById(userId);
        return user == null ? null : buildBrief(user);
    }

    public UserProfileVO getProfile(Long userId) {
        User user = authMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        UserProfile profile = getByUserId(userId);

        UserProfileVO vo = new UserProfileVO();
        vo.setUid(user.getUid());
        vo.setRole(user.getRole());
        vo.setRoleDesc(user.getRole() == null ? null : user.getRole().getDesc());
        vo.setStatus(user.getStatus());
        vo.setMaskedPhone(MaskUtil.maskPhone(user.getPhone()));
        if (profile != null) {
            vo.setNickname(profile.getNickname());
            vo.setAvatarUrl(profile.getAvatarUrl());
            vo.setGender(profile.getGender());
            vo.setGenderDesc(profile.getGender() == null ? null : profile.getGender().getDesc());
        }
        return vo;
    }

    /**
     * 部分更新：仅非空字段入库（updateById 默认 NOT_NULL 字段策略）。
     */
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setNickname(dto.getNickname());
        profile.setAvatarUrl(dto.getAvatarUrl());
        profile.setGender(dto.getGender());
        userProfileMapper.updateById(profile);
    }
}
