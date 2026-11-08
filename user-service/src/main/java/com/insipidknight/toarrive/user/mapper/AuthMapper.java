package com.insipidknight.toarrive.user.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insipidknight.toarrive.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE phone = #{phone} AND delete_at = '1970-01-01 00:00:00'")
    User selectByPhone(String phone);

    @Update("UPDATE user SET role = #{role} WHERE user_id = #{userId} AND delete_at = '1970-01-01 00:00:00'")
    int updateRole(@Param("userId") Long userId, @Param("role") Integer role);

    @Update("UPDATE user SET status = #{status} WHERE user_id = #{userId} AND delete_at = '1970-01-01 00:00:00'")
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
