package com.example.TradeStream.cryptoCoinService.service;

import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface CoinService {
    List<Coin> getAllCoins(int page);
    JsonNode getMarketChart(String coinId, int days);
    JsonNode getCoinDetails(String coinId);
    Coin getCoinById(String coinId);
    JsonNode searchCoins(String query);
    JsonNode getTop50CoinsByMarketCapRank();
    JsonNode getTrendingCoins();
}
