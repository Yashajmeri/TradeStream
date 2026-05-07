package com.example.TradeStream.marketService.repositories;

import com.example.TradeStream.marketService.entity.Candle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandleRepository extends JpaRepository<Candle, Long> {
    List<Candle> findBySymbolAndResolutionAndTimestampBetween(
            String symbol, String resolution, Long from, Long to);
}
