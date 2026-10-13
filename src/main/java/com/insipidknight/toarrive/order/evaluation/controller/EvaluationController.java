package com.insipidknight.toarrive.order.evaluation.controller;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.order.evaluation.dto.EvaluationCreateDTO;
import com.insipidknight.toarrive.order.evaluation.service.EvaluationService;
import com.insipidknight.toarrive.order.evaluation.vo.EvaluationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/{orderNo}/evaluate")
    public Result<EvaluationVO> evaluate(@PathVariable String orderNo,
                                         @RequestBody @Valid EvaluationCreateDTO evaluationCreateDTO) {
        return Result.success(evaluationService.evaluate(orderNo, UserContext.getUserId(), evaluationCreateDTO));
    }
}
