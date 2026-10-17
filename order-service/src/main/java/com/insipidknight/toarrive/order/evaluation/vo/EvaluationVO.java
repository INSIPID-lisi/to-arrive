package com.insipidknight.toarrive.order.evaluation.vo;

import com.insipidknight.toarrive.order.evaluation.entity.Evaluation;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EvaluationVO {

    private String orderNo;

    private Integer score;

    private String comment;

    private String tags;

    private LocalDateTime createTime;

    public EvaluationVO(Evaluation evaluation) {
        this.orderNo = evaluation.getOrderNo();
        this.score = evaluation.getScore();
        this.comment = evaluation.getComment();
        this.tags = evaluation.getTags();
        this.createTime = evaluation.getCreateTime();
    }
}
