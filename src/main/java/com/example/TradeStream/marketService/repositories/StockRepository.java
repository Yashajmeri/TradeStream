package com.example.TradeStream.marketService.repositories;

import com.example.TradeStream.marketService.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);
    List<Stock> findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(
            String symbol, String name);
}
