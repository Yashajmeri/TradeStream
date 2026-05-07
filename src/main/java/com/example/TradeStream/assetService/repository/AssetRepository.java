package com.example.TradeStream.assetService.repository;

import com.example.TradeStream.assetService.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByUser_IdAndId(Long userId, Long assetId);

    List<Asset> findByUser_Id(Long userId);

    Optional<Asset> findByUser_IdAndCoin_CoinId(Long userId, String coinId);
}
