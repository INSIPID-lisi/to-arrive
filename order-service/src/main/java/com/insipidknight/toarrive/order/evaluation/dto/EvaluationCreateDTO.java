package com.insipidknight.toarrive.order.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EvaluationCreateDTO {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围为1-5")
    @Max(value = 5, message = "评分范围为1-5")
    private Integer score;

    @Size(max = 255, message = "评论长度不能超过255位")
    private String comment;

    @Size(max = 255, message = "标签长度不能超过255位")
    private String tags;
}
