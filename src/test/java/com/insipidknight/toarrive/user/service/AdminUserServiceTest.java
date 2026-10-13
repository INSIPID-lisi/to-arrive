package com.insipidknight.toarrive.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.user.entity.User;
import com.insipidknight.toarrive.user.entity.UserProfile;
import com.insipidknight.toarrive.user.entity.enums.UserRole;
import com.insipidknight.toarrive.user.entity.enums.UserStatus;
import com.insipidknight.toarrive.user.mapper.AuthMapper;
import com.insipidknight.toarrive.user.vo.AdminUserVO;
import com.insipidknight.toarrive.user.vo.PageVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("listUsers：返回 total/page/size 与逐条组装的 VO")
    void listUsers_success() {
        User user = user(5L, UserRole.PASSENGER, UserStatus.NORMAL);
        when(authMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(authMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user));

        UserProfile profile = new UserProfile();
        profile.setNickname("小明");
        when(userProfileService.getByUserId(5L)).thenReturn(profile);

        PageVO<AdminUserVO> page = adminUserService.listUsers(UserRole.PASSENGER, UserStatus.NORMAL, "138", 2, 10);

        assertThat(page.getTotal()).isEqualTo(1L);
        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).getUserId()).isEqualTo(5L);
        assertThat(page.getList().get(0).getNickname()).isEqualTo("小明");
        assertThat(page.getList().get(0).getMaskedPhone()).isEqualTo("138****8000");
    }

    @Test
    @DisplayName("listUsers：size 超上限时被夹到 100，page 非法时归 1")
    void listUsers_clampsParams() {
        when(authMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(authMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        PageVO<AdminUserVO> page = adminUserService.listUsers(null, null, null, 0, 999);

        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(100);
        assertThat(page.getList()).isEmpty();
    }

    @Test
    @DisplayName("disable：目标账号不存在 → 报错，不更新")
    void disable_userNotFound() {
        when(authMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> adminUserService.disable(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号不存在");

        verify(authMapper, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("disable：目标为管理员 → 拒绝，防互锁")
    void disable_adminRejected() {
        when(authMapper.selectById(1L)).thenReturn(user(1L, UserRole.ADMIN, UserStatus.NORMAL));

        assertThatThrownBy(() -> adminUserService.disable(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能禁用管理员账号");

        verify(authMapper, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("disable：正常账号 → 置 DISABLED")
    void disable_success() {
        when(authMapper.selectById(1L)).thenReturn(user(1L, UserRole.PASSENGER, UserStatus.NORMAL));

        adminUserService.disable(1L);

        verify(authMapper).updateStatus(1L, UserStatus.DISABLED.getValue());
    }

    @Test
    @DisplayName("enable：置回 NORMAL")
    void enable_success() {
        when(authMapper.selectById(1L)).thenReturn(user(1L, UserRole.PASSENGER, UserStatus.DISABLED));

        adminUserService.enable(1L);

        verify(authMapper).updateStatus(1L, UserStatus.NORMAL.getValue());
    }

    private static User user(Long id, UserRole role, UserStatus status) {
        User user = new User();
        user.setUserId(id);
        user.setUid("U0000000000" + id);
        user.setPhone("13800138000");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
