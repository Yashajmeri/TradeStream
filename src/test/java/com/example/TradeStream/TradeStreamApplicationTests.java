package com.example.TradeStream;

import com.example.TradeStream.assetService.service.AssetService;
import com.example.TradeStream.orderService.service.OrderService;
import com.example.TradeStream.walletService.service.WalletService;
import com.example.TradeStream.watchListService.service.WatchListService;
import com.example.TradeStream.withdrawalService.service.WithdrawalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TradeStreamApplicationTests {

    @Autowired private WalletService walletService;
    @Autowired private OrderService orderService;
    @Autowired private AssetService assetService;
    @Autowired private WatchListService watchListService;
    @Autowired private WithdrawalService withdrawalService;

    @Test
    void contextLoads() {
    }

    @Test
    void allCoreServicesAreWired() {
        assertThat(walletService).isNotNull();
        assertThat(orderService).isNotNull();
        assertThat(assetService).isNotNull();
        assertThat(watchListService).isNotNull();
        assertThat(withdrawalService).isNotNull();
    }
}
