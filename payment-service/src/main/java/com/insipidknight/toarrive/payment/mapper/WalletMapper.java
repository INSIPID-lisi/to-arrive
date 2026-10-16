package com.insipidknight.toarrive.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insipidknight.toarrive.payment.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {

    @Select("SELECT * FROM wallet WHERE user_id = #{userId}")
    Wallet selectByUserId(@Param("userId") Long userId);

    /**
     * 入账：钱包不存在则惰性创建，存在则累加（原子）
     */
    @Update("INSERT INTO wallet (user_id, balance) VALUES (#{userId}, #{amount}) " +
            "ON DUPLICATE KEY UPDATE balance = balance + #{amount}")
    int credit(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 扣款：余额充足才扣（原子条件 UPDATE），返回 0 行表示余额不足
     */
    @Update("UPDATE wallet SET balance = balance - #{amount} " +
            "WHERE user_id = #{userId} AND balance >= #{amount}")
    int deduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
