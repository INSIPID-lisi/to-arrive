package com.insipidknight.toarrive.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.user.entity.User;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.toarrive.user.entity.enums.UserStatus;
import com.insipidknight.toarrive.user.mapper.AuthMapper;
import com.insipidknight.toarrive.user.vo.AdminUserVO;
import com.insipidknight.toarrive.user.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int MAX_SIZE = 100;

    private final AuthMapper authMapper;
    private final UserProfileService userProfileService;

    public PageVO<AdminUserVO> listUsers(UserRole role, UserStatus status, String phoneKeyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(size, 1, MAX_SIZE);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(role != null, User::getRole, role);
        wrapper.eq(status != null, User::getStatus, status);
        wrapper.like(StringUtils.hasText(phoneKeyword), User::getPhone, phoneKeyword);
        wrapper.orderByDesc(User::getUserId);

        long total = authMapper.selectCount(wrapper);

        int offset = (safePage - 1) * safeSize;
        wrapper.last("LIMIT " + offset + ", " + safeSize);
        List<AdminUserVO> list = authMapper.selectList(wrapper).stream()
                .map(user -> new AdminUserVO(user, userProfileService.getByUserId(user.getUserId())))
                .toList();

        return new PageVO<>(list, total, safePage, safeSize);
    }

    public void disable(Long userId) {
        User user = requireUser(userId);
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("不能禁用管理员账号");
        }
        authMapper.updateStatus(userId, UserStatus.DISABLED.getValue());
    }

    public void enable(Long userId) {
        requireUser(userId);
        authMapper.updateStatus(userId, UserStatus.NORMAL.getValue());
    }

    private User requireUser(Long userId) {
        User user = authMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        return user;
    }
}
