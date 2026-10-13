package com.insipidknight.toarrive.user.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.user.dto.UpdateProfileDTO;
import com.insipidknight.toarrive.user.entity.User;
import com.insipidknight.toarrive.user.entity.UserProfile;
import com.insipidknight.toarrive.user.entity.enums.Gender;
import com.insipidknight.toarrive.user.entity.enums.UserRole;
import com.insipidknight.toarrive.user.entity.enums.UserStatus;
import com.insipidknight.toarrive.user.mapper.AuthMapper;
import com.insipidknight.toarrive.user.mapper.UserProfileMapper;
import com.insipidknight.toarrive.user.vo.UserProfileVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("getProfile：账号不存在 → 报错")
    void getProfile_userNotFound() {
        when(authMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> userProfileService.getProfile(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号不存在");
    }

    @Test
    @DisplayName("getProfile：汇总 User + UserProfile（含脱敏手机号、角色与性别描述）")
    void getProfile_success() {
        User user = new User();
        user.setUserId(1L);
        user.setUid("U00000000001");
        user.setPhone("13800138000");
        user.setRole(UserRole.PASSENGER);
        user.setStatus(UserStatus.NORMAL);
        when(authMapper.selectById(1L)).thenReturn(user);

        UserProfile profile = new UserProfile();
        profile.setNickname("小明");
        profile.setGender(Gender.MALE);
        when(userProfileMapper.selectByUserId(1L)).thenReturn(profile);

        UserProfileVO vo = userProfileService.getProfile(1L);

        assertThat(vo.getUid()).isEqualTo("U00000000001");
        assertThat(vo.getNickname()).isEqualTo("小明");
        assertThat(vo.getGender()).isEqualTo(Gender.MALE);
        assertThat(vo.getGenderDesc()).isEqualTo("男");
        assertThat(vo.getRole()).isEqualTo(UserRole.PASSENGER);
        assertThat(vo.getRoleDesc()).isEqualTo("乘客");
        assertThat(vo.getMaskedPhone()).isEqualTo("138****8000");
    }

    @Test
    @DisplayName("updateProfile：仅非空字段入库（部分更新，空字段保持 null）")
    void updateProfile_partialUpdate() {
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("新昵称");

        userProfileService.updateProfile(1L, dto);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper).updateById(captor.capture());
        UserProfile saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getNickname()).isEqualTo("新昵称");
        assertThat(saved.getAvatarUrl()).isNull();
        assertThat(saved.getGender()).isNull();
    }
}
