package com.insipidknight.toarrive.wallet.controller;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.wallet.dto.RechargeDTO;
import com.insipidknight.toarrive.wallet.service.WalletService;
import com.insipidknight.toarrive.wallet.vo.WalletVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public Result<WalletVO> myWallet() {
        Long userId = UserContext.getUserId();
        return Result.success(new WalletVO(userId, walletService.getBalance(userId)));
    }

    @PostMapping("/recharge")
    public Result<WalletVO> recharge(@RequestBody @Valid RechargeDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(new WalletVO(userId, walletService.recharge(userId, dto.getAmount())));
    }
}
