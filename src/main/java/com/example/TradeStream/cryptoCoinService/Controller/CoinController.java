package com.example.TradeStream.cryptoCoinService.Controller;

import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.cryptoCoinService.service.CoinService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Crypto Coins", description = "CoinGecko market data — prices, charts, search, trending")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coins")
public class CoinController {
    private final CoinService coinService;

    @GetMapping
    ResponseEntity<List<Coin>> getAllCoins(@RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(coinService.getAllCoins( page));
    }
    @GetMapping("/{coinId}/chart")
    ResponseEntity<JsonNode> getMarketChart (@PathVariable String coinId, @RequestParam(defaultValue ="7" ) int days)  {
         JsonNode response = coinService.getMarketChart(coinId, days);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{coinId}")
    public ResponseEntity<Coin> getCoinById(@PathVariable String coinId) {
        return ResponseEntity.ok(coinService.getCoinById(coinId));
    }
    @GetMapping("details/{coinId}")
    public ResponseEntity<JsonNode> getCoinDetailsById(@PathVariable String coinId) {
        return ResponseEntity.ok(coinService.getCoinDetails(coinId));
    }


    @GetMapping("/search")
    public ResponseEntity<JsonNode> searchCoins(@RequestParam String query) {
        return ResponseEntity.ok(coinService.searchCoins(query));
    }

    @GetMapping("/top50")
    public ResponseEntity<JsonNode> getTop50CoinsByMarketCapRank() {
        return ResponseEntity.ok(coinService.getTop50CoinsByMarketCapRank());
    }

    @GetMapping("/trending")
    public ResponseEntity<JsonNode> getTrendingCoins() {
        return ResponseEntity.ok(coinService.getTrendingCoins());
    }

}
