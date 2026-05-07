package com.example.TradeStream.walletService.service;
import com.example.TradeStream.orderService.entity.Order;
import com.example.TradeStream.orderService.payload.OrderType;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.entity.Wallet;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.walletService.payload.WalletTransactionStatus;
import com.example.TradeStream.walletService.payload.WalletTransactionType;
import com.example.TradeStream.walletService.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionService walletTransactionService;
    @Override
    public Wallet getWalletByUser(User user) {
        Wallet wallet = walletRepository.findByUser_Id(user.getId());
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUser(user);
            wallet.setBalance(BigDecimal.ZERO);
            walletRepository.save(wallet);
        }

        return wallet;
    }

    @Override
    public Wallet addFunds(Wallet wallet, BigDecimal amount) {
        BigDecimal balanceBefore = wallet.getBalance();

        BigDecimal balanceAfter = balanceBefore.add(amount);
        wallet.setBalance(balanceAfter);
        Wallet savedWallet = walletRepository.save(wallet);
        walletTransactionService.createTransaction(
                savedWallet,
                savedWallet.getUser(),
                WalletTransactionType.ADD_FUNDS,
                WalletTransactionStatus.SUCCESS,
                amount,
                balanceBefore,
                balanceAfter,
                "Added funds to wallet",
                null);
        return savedWallet;
    }

    @Transactional(readOnly = true)
    @Override
    public Wallet findWalletById(Long walletId) {
        Optional<Wallet> walletOptional = walletRepository.findById(walletId);
        if (walletOptional.isEmpty()) {
            throw new APIException("Wallet not found with id: " + walletId);
        }
        return walletOptional.get();
    }

    @Override
    public Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new APIException("Amount must be greater than zero");
        }

        Wallet senderWallet = getWalletByUser(sender);

        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new APIException("Insufficient funds in sender's wallet");
        }

        BigDecimal senderBefore = senderWallet.getBalance();
        BigDecimal senderAfter = senderBefore.subtract(amount);
        senderWallet.setBalance(senderAfter);
        walletRepository.save(senderWallet);

        BigDecimal receiverBefore = receiverWallet.getBalance();
        BigDecimal receiverAfter = receiverBefore.add(amount);
        receiverWallet.setBalance(receiverAfter);
        walletRepository.save(receiverWallet);

        String transferRef = "TRF-" + System.currentTimeMillis();

        walletTransactionService.createTransaction(
                senderWallet,
                sender,
                WalletTransactionType.TRANSFER_SENT,
                WalletTransactionStatus.SUCCESS,
                amount,
                senderBefore,
                senderAfter,
                "Wallet to wallet transfer sent",
                transferRef
        );

        walletTransactionService.createTransaction(
                receiverWallet,
                receiverWallet.getUser(),
                WalletTransactionType.TRANSFER_RECEIVED,
                WalletTransactionStatus.SUCCESS,
                amount,
                receiverBefore,
                receiverAfter,
                "Wallet to wallet transfer received",
                transferRef
        );
        return senderWallet;
    }



    @Transactional
    @Override
    public Wallet doOrderPayment(User user, Order order) {

        Wallet userWallet = getWalletByUser(user);
        BigDecimal currentBalance = userWallet.getBalance();
        BigDecimal balanceAfter;

        if (order.getOrderType() == OrderType.BUY) {
            if (currentBalance.compareTo(order.getAmount()) < 0) {
                throw new APIException("Insufficient funds in wallet to complete the order");
            }

            balanceAfter = currentBalance.subtract(order.getAmount());
            userWallet.setBalance(balanceAfter);
            Wallet savedWallet = walletRepository.save(userWallet);

            walletTransactionService.createTransaction(
                    savedWallet,
                    user,
                    WalletTransactionType.BUY_ASSET,
                    WalletTransactionStatus.SUCCESS,
                    order.getAmount(),
                    currentBalance,
                    balanceAfter,
                    "Wallet debited for buy order",
                    String.valueOf(order.getId())

            );

            return savedWallet;

        } else if (order.getOrderType() == OrderType.SELL) {
            balanceAfter = currentBalance.add(order.getAmount());
            userWallet.setBalance(balanceAfter);
            Wallet savedWallet = walletRepository.save(userWallet);

            walletTransactionService.createTransaction(
                    savedWallet,
                    user,
                    WalletTransactionType.SELL_ASSET,
                    WalletTransactionStatus.SUCCESS,
                    order.getAmount(),
                    currentBalance,
                    balanceAfter,
                    "Wallet credited for sell order",
                    String.valueOf(order.getId()));

            return savedWallet;
        } else {
            throw new APIException("Invalid order type");
        }

    }
}