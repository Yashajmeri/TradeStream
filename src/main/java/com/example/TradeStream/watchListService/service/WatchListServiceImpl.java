package com.example.TradeStream.watchListService.service;

import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.watchListService.entity.WatchList;
import com.example.TradeStream.watchListService.repository.WatchListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchListServiceImpl implements WatchListService {
    private final WatchListRepository watchListRepository;
    @Override
    public WatchList getOrCreateWatchListByUser(User user) {
        return watchListRepository.findByUser_Id(user.getId())
                .orElseGet(() -> createWatchListForUser(user));
    }

    @Override
    public WatchList createWatchListForUser(User user) {
            WatchList watchList = getWatchListByUser(user);
            if(watchList == null){
            watchList = new WatchList();
            watchList.setUser(user);
           }
        return watchListRepository.save(watchList);
    }

    @Transactional(readOnly = true)
    @Override
    public WatchList findWatchListById(Long WatchListId) {
        return watchListRepository.findById(WatchListId)
                .orElseThrow(() -> new APIException("WatchList not found with id: " + WatchListId));
    }

    @Override
    public WatchList addCoinToWatchList(User user, Coin coin) {
            WatchList watchList = getOrCreateWatchListByUser(user);
            if(watchList.getCoins().contains(coin)){
                throw new APIException("Coin already in watchlist");
            }
            watchList.getCoins().add(coin);
            return watchListRepository.save(watchList);
    }

    @Override
    public WatchList removeCoinFromWatchList(User user, Coin coin) {
        WatchList watchList = getOrCreateWatchListByUser(user);
        if(!watchList.getCoins().contains(coin)){
            throw new APIException("Coin not found in watchlist"); }
        watchList.getCoins().remove(coin);
        return watchListRepository.save(watchList);
    }
    private WatchList getWatchListByUser(User user) {
        return watchListRepository.findByUser_Id(user.getId())
                .orElse(null);
    }
}
