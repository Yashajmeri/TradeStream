package com.example.TradeStream.assetService.service;

import com.example.TradeStream.assetService.entity.Asset;
import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.userService.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface AssetService {
    Asset createAsset(User user, Coin coin, BigDecimal quantity);

    Asset getAssetById(Long assetId);

    Asset getAssetByUserIdAndId(Long userId, Long assetId);

    List<Asset> getAssetsByUserId(Long userId);

    Asset updateAsset(Long assetId, BigDecimal quantity);

    Asset findAssetByUserIdAndCoinId(Long userId, String coinId);

    void deleteAsset(Long assetId);
}
