package com.insipidknight.toarrive.user.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insipidknight.toarrive.user.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    @Select("SELECT * FROM user_profile WHERE user_id = #{userId} AND delete_at = '1970-01-01 00:00:00'")
    UserProfile selectByUserId(Long userId);
}
