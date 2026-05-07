package com.example.TradeStream.watchListService;

import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.watchListService.entity.WatchList;
import com.example.TradeStream.watchListService.repository.WatchListRepository;
import com.example.TradeStream.watchListService.service.WatchListServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchListServiceImplTest {

    @Mock
    private WatchListRepository watchListRepository;

    @InjectMocks
    private WatchListServiceImpl watchListService;

    private User user;
    private Coin bitcoin;
    private WatchList watchList;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("password123");

        bitcoin = new Coin();
        bitcoin.setCoinId("bitcoin");
        bitcoin.setSymbol("BTC");
        bitcoin.setName("Bitcoin");
        bitcoin.setCurrentPrice(50000.0);

        watchList = new WatchList();
        watchList.setId(1L);
        watchList.setUser(user);
        watchList.setCoins(new ArrayList<>());
    }

    // --- getOrCreateWatchListByUser ---

    @Test
    void getOrCreateWatchListByUser_whenExists_returnsExistingWatchList() {
        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.of(watchList));

        WatchList result = watchListService.getOrCreateWatchListByUser(user);

        assertThat(result).isEqualTo(watchList);
        verify(watchListRepository, never()).save(any());
    }

    @Test
    void getOrCreateWatchListByUser_whenNotExists_createsNewWatchList() {
        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(watchListRepository.save(any(WatchList.class))).thenAnswer(inv -> {
            WatchList wl = inv.getArgument(0);
            wl.setId(2L);
            return wl;
        });

        WatchList result = watchListService.getOrCreateWatchListByUser(user);

        assertThat(result.getUser()).isEqualTo(user);
        verify(watchListRepository).save(any(WatchList.class));
    }

    // --- createWatchListForUser ---

    @Test
    void createWatchListForUser_whenNoneExists_savesNewWatchList() {
        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(watchListRepository.save(any(WatchList.class))).thenAnswer(inv -> {
            WatchList wl = inv.getArgument(0);
            wl.setId(1L);
            return wl;
        });

        WatchList result = watchListService.createWatchListForUser(user);

        assertThat(result.getUser()).isEqualTo(user);
        verify(watchListRepository).save(any(WatchList.class));
    }

    @Test
    void createWatchListForUser_whenAlreadyExists_savesExistingWatchList() {
        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.of(watchList));
        when(watchListRepository.save(watchList)).thenReturn(watchList);

        WatchList result = watchListService.createWatchListForUser(user);

        assertThat(result).isEqualTo(watchList);
    }

    // --- findWatchListById ---

    @Test
    void findWatchListById_whenFound_returnsWatchList() {
        when(watchListRepository.findById(1L)).thenReturn(Optional.of(watchList));

        WatchList result = watchListService.findWatchListById(1L);

        assertThat(result).isEqualTo(watchList);
    }

    @Test
    void findWatchListById_whenNotFound_throwsAPIException() {
        when(watchListRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchListService.findWatchListById(99L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("99");
    }

    // --- addCoinToWatchList ---

    @Test
    void addCoinToWatchList_success_addsCoinToList() {
        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.of(watchList));
        when(watchListRepository.save(watchList)).thenReturn(watchList);

        WatchList result = watchListService.addCoinToWatchList(user, bitcoin);

        assertThat(result.getCoins()).contains(bitcoin);
    }

    @Test
    void addCoinToWatchList_duplicateCoin_throwsAPIException() {
        watchList.getCoins().add(bitcoin);
        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.of(watchList));

        assertThatThrownBy(() -> watchListService.addCoinToWatchList(user, bitcoin))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("already in watchlist");
    }

    @Test
    void addCoinToWatchList_multipleCoins_allAdded() {
        Coin ethereum = new Coin();
        ethereum.setCoinId("ethereum");
        ethereum.setSymbol("ETH");
        ethereum.setName("Ethereum");
        ethereum.setCurrentPrice(3000.0);

        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.of(watchList));
        when(watchListRepository.save(watchList)).thenReturn(watchList);

        watchListService.addCoinToWatchList(user, bitcoin);
        watchList.getCoins().add(bitcoin); // reflect state for second call
        watchListService.addCoinToWatchList(user, ethereum);

        assertThat(watchList.getCoins()).contains(bitcoin, ethereum);
    }

    // --- removeCoinFromWatchList ---

    @Test
    void removeCoinFromWatchList_success_removesCoinFromList() {
        watchList.getCoins().add(bitcoin);
        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.of(watchList));
        when(watchListRepository.save(watchList)).thenReturn(watchList);

        WatchList result = watchListService.removeCoinFromWatchList(user, bitcoin);

        assertThat(result.getCoins()).doesNotContain(bitcoin);
    }

    @Test
    void removeCoinFromWatchList_coinNotInList_throwsAPIException() {
        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.of(watchList));

        assertThatThrownBy(() -> watchListService.removeCoinFromWatchList(user, bitcoin))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("not found in watchlist");
    }

    @Test
    void removeCoinFromWatchList_leavesOtherCoinsIntact() {
        Coin ethereum = new Coin();
        ethereum.setCoinId("ethereum");
        ethereum.setSymbol("ETH");
        ethereum.setName("Ethereum");
        ethereum.setCurrentPrice(3000.0);

        watchList.getCoins().add(bitcoin);
        watchList.getCoins().add(ethereum);
        when(watchListRepository.findByUser_Id(1L)).thenReturn(Optional.of(watchList));
        when(watchListRepository.save(watchList)).thenReturn(watchList);

        WatchList result = watchListService.removeCoinFromWatchList(user, bitcoin);

        assertThat(result.getCoins()).contains(ethereum);
        assertThat(result.getCoins()).doesNotContain(bitcoin);
    }
}
