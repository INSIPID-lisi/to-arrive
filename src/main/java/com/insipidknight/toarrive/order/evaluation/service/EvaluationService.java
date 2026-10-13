package com.insipidknight.toarrive.order.evaluation.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.entity.enums.PayStatus;
import com.insipidknight.toarrive.order.evaluation.dto.EvaluationCreateDTO;
import com.insipidknight.toarrive.order.evaluation.entity.Evaluation;
import com.insipidknight.toarrive.order.evaluation.mapper.EvaluationMapper;
import com.insipidknight.toarrive.order.evaluation.vo.EvaluationVO;
import com.insipidknight.toarrive.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final OrderMapper orderMapper;

    /**
     * 乘客评价司机：仅订单所属乘客、且订单已完成并已支付时，一单一评。
     */
    @Transactional
    public EvaluationVO evaluate(String orderNo, Long userId, EvaluationCreateDTO dto) {

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!userId.equals(order.getPassengerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有乘客可以评价该订单");
        }
        if (order.getStatus() != OrderStatus.FINISHED) {
            throw new BusinessException("订单未完成，无法评价");
        }
        if (order.getPayStatus() != PayStatus.PAID) {
            throw new BusinessException("订单未支付，无法评价");
        }
        if (evaluationMapper.selectByOrderNo(orderNo) != null) {
            throw new BusinessException("该订单已评价");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setOrderNo(orderNo);
        evaluation.setPassengerId(userId);
        evaluation.setDriverId(order.getDriverId());
        evaluation.setScore(dto.getScore());
        evaluation.setComment(dto.getComment());
        evaluation.setTags(dto.getTags());

        try {
            evaluationMapper.insert(evaluation);
        } catch (DuplicateKeyException e) {
            // 并发下唯一键 uk_order_no 兜底，保证一单一评
            throw new BusinessException("该订单已评价");
        }
        return new EvaluationVO(evaluation);
    }
}
