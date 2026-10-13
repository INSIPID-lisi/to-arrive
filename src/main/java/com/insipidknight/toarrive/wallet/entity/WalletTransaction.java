package com.insipidknight.toarrive.wallet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.insipidknight.toarrive.wallet.entity.enums.TxType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wallet_transaction")
public class WalletTransaction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    // 充值类流水无订单号，可为空
    private String orderNo;

    private TxType type;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
