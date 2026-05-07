package com.example.TradeStream.assetService.service;

import com.example.TradeStream.assetService.entity.Asset;
import com.example.TradeStream.assetService.exception.ResourceNotFoundException;
import com.example.TradeStream.assetService.repository.AssetRepository;
import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.userService.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {
    private final AssetRepository assetRepository;

    @Override
    public Asset createAsset(User user, Coin coin, BigDecimal quantity) {
        Asset asset = new Asset();
        asset.setUser(user);
        asset.setCoin(coin);
        asset.setQuantity(quantity);
        asset.setBuyPrice(BigDecimal.valueOf(coin.getCurrentPrice()));
        return assetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    @Override
    public Asset getAssetById(Long assetId) {
        return assetRepository.findById(assetId).orElseThrow(()->new ResourceNotFoundException("Asset", "id", assetId));
    }

    @Transactional(readOnly = true)
    @Override
    public Asset getAssetByUserIdAndId(Long userId, Long assetId) {
        return assetRepository.findByUser_IdAndId(userId, assetId).orElseThrow(()->new ResourceNotFoundException("Asset", "id", assetId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<Asset> getAssetsByUserId(Long userId) {
        return assetRepository.findByUser_Id(userId);
    }

    @Override
    public Asset updateAsset(Long assetId, BigDecimal quantity) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", assetId));
        asset.setQuantity(asset.getQuantity().add(quantity));
        return assetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    @Override
    public Asset findAssetByUserIdAndCoinId(Long userId, String coinId) {
        return assetRepository.findByUser_IdAndCoin_CoinId(userId, coinId).orElse(null);
    }

    @Override
    public void deleteAsset(Long assetId) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(()->new ResourceNotFoundException("Asset", "id", assetId));
        assetRepository.delete(asset);
    }
}
