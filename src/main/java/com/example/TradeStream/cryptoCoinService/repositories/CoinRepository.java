package com.example.TradeStream.cryptoCoinService.repositories;

import com.example.TradeStream.cryptoCoinService.entity.Coin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoinRepository extends JpaRepository<Coin,String> {

    Optional<Coin> findByCoinId(String coinId);
}
