package com.example.TradeStream.watchListService.repository;

import com.example.TradeStream.watchListService.entity.WatchList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WatchListRepository extends JpaRepository<WatchList, Long> {
    Optional<WatchList> findByUser_Id(Long userId);
}
