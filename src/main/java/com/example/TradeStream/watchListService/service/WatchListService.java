package com.example.TradeStream.watchListService.service;

import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.watchListService.entity.WatchList;

public interface WatchListService {
      WatchList getOrCreateWatchListByUser(User user);
      WatchList createWatchListForUser(User user);
      WatchList findWatchListById(Long WatchListId);
      WatchList addCoinToWatchList(User user, Coin coin);
        WatchList removeCoinFromWatchList(User user, Coin coin);

}
