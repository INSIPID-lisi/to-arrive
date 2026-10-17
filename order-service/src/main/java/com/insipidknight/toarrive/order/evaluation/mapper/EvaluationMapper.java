package com.insipidknight.toarrive.order.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insipidknight.toarrive.order.evaluation.entity.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EvaluationMapper extends BaseMapper<Evaluation> {

    @Select("SELECT * FROM evaluation WHERE order_no = #{orderNo}")
    Evaluation selectByOrderNo(String orderNo);
}
