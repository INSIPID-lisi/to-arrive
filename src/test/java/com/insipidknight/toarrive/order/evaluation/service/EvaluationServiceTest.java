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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    private static final String ORDER_NO = "TA202610130000000001";
    private static final Long PASSENGER_ID = 100L;
    private static final Long DRIVER_ID = 200L;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private EvaluationService evaluationService;

    @Test
    @DisplayName("评价：订单不存在 → 报错，不落库")
    void evaluate_orderNotFound() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(null);

        assertThatThrownBy(() -> evaluationService.evaluate(ORDER_NO, PASSENGER_ID, dto()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单不存在");

        verify(evaluationMapper, never()).insert(any(Evaluation.class));
    }

    @Test
    @DisplayName("评价：非订单乘客 → FORBIDDEN，不落库")
    void evaluate_notPassenger() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order(OrderStatus.FINISHED, PayStatus.PAID));

        assertThatThrownBy(() -> evaluationService.evaluate(ORDER_NO, 999L, dto()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN.getCode())
                .hasMessage("只有乘客可以评价该订单");

        verify(evaluationMapper, never()).insert(any(Evaluation.class));
    }

    @Test
    @DisplayName("评价：订单未完成 → 报错")
    void evaluate_notFinished() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order(OrderStatus.IN_TRIP, PayStatus.UNPAID));

        assertThatThrownBy(() -> evaluationService.evaluate(ORDER_NO, PASSENGER_ID, dto()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单未完成，无法评价");

        verify(evaluationMapper, never()).insert(any(Evaluation.class));
    }

    @Test
    @DisplayName("评价：订单未支付 → 报错")
    void evaluate_notPaid() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order(OrderStatus.FINISHED, PayStatus.UNPAID));

        assertThatThrownBy(() -> evaluationService.evaluate(ORDER_NO, PASSENGER_ID, dto()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单未支付，无法评价");

        verify(evaluationMapper, never()).insert(any(Evaluation.class));
    }

    @Test
    @DisplayName("评价：已评价过 → 报错，不重复落库")
    void evaluate_alreadyEvaluated() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order(OrderStatus.FINISHED, PayStatus.PAID));
        when(evaluationMapper.selectByOrderNo(ORDER_NO)).thenReturn(new Evaluation());

        assertThatThrownBy(() -> evaluationService.evaluate(ORDER_NO, PASSENGER_ID, dto()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该订单已评价");

        verify(evaluationMapper, never()).insert(any(Evaluation.class));
    }

    @Test
    @DisplayName("评价成功：落库携带订单/司机/评分，返回 VO")
    void evaluate_success() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order(OrderStatus.FINISHED, PayStatus.PAID));
        when(evaluationMapper.selectByOrderNo(ORDER_NO)).thenReturn(null);

        EvaluationVO vo = evaluationService.evaluate(ORDER_NO, PASSENGER_ID, dto());

        ArgumentCaptor<Evaluation> captor = ArgumentCaptor.forClass(Evaluation.class);
        verify(evaluationMapper).insert(captor.capture());
        Evaluation saved = captor.getValue();
        assertThat(saved.getOrderNo()).isEqualTo(ORDER_NO);
        assertThat(saved.getPassengerId()).isEqualTo(PASSENGER_ID);
        assertThat(saved.getDriverId()).isEqualTo(DRIVER_ID);
        assertThat(saved.getScore()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("很好");
        assertThat(saved.getTags()).isEqualTo("准时,态度好");

        assertThat(vo.getOrderNo()).isEqualTo(ORDER_NO);
        assertThat(vo.getScore()).isEqualTo(5);
    }

    private static EvaluationCreateDTO dto() {
        EvaluationCreateDTO dto = new EvaluationCreateDTO();
        dto.setScore(5);
        dto.setComment("很好");
        dto.setTags("准时,态度好");
        return dto;
    }

    private static Order order(OrderStatus status, PayStatus payStatus) {
        Order order = new Order();
        order.setOrderNo(ORDER_NO);
        order.setPassengerId(PASSENGER_ID);
        order.setDriverId(DRIVER_ID);
        order.setStatus(status);
        order.setPayStatus(payStatus);
        return order;
    }
}
