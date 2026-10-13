package com.insipidknight.toarrive.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insipidknight.toarrive.wallet.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {
}
