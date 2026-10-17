package com.insipidknight.toarrive.api.util;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;

/**
 * 内部端点沿用 {@link Result} 作为统一返回体，且业务异常也是 HTTP 200，
 * 消费方需显式判断 code 才能还原异常语义。
 */
public final class ClientResults {

    private ClientResults() {
    }

    public static <T> T unwrap(Result<T> result) {
        if (result.getCode() != ErrorCode.SUCCESS.getCode()) {
            throw new BusinessException(result.getMessage());
        }
        return result.getData();
    }
}
