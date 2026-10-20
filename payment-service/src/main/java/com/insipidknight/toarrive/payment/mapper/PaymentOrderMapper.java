package com.insipidknight.toarrive.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insipidknight.toarrive.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
