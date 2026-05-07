package com.example.TradeStream.cryptoCoinService.service;

import com.example.TradeStream.common.ExternalApiRateLimiter;
import com.example.TradeStream.cryptoCoinService.apiResponseDto.CoinGeckoMarketResponseDTO;
import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.cryptoCoinService.exception.APIException;
import com.example.TradeStream.cryptoCoinService.repositories.CoinRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CoinServiceImpl implements CoinService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final CoinRepository coinRepository;
    private final ExternalApiRateLimiter externalApiRateLimiter;

    @Value("${coingecko.api.url}")
    private String baseUrl;

    @Override
    public List<Coin> getAllCoins(int page) {
        String url = baseUrl + "/coins/markets"
                + "?vs_currency=usd"
                + "&order=market_cap_desc"
                + "&per_page=10"
                + "&page=" + page
                + "&sparkline=false";

        CoinGeckoMarketResponseDTO[] response = fetchAndParseArray(url);

        if (response.length == 0) {
            throw new APIException("No coins returned from CoinGecko");
        }

        return Arrays.stream(response)
                .map(this::mapToCoinEntity)
                .toList();
    }

    @Override
    public JsonNode getMarketChart(String coinId, int days) {
        String url = baseUrl + "/coins/" + coinId + "/market_chart"
                + "?vs_currency=usd"
                + "&days=" + days;

        return fetchJsonResponse(url, "Failed to fetch market chart for coin: " + coinId);
    }

    @Override
    public JsonNode getCoinDetails(String coinId) {
        String url = baseUrl + "/coins/" + coinId;

        JsonNode jsonNode = fetchJsonResponse(url, "Failed to fetch coin details for coin: " + coinId);

        Coin coin = mapCoinDetails(jsonNode);
        coinRepository.save(coin);

        return jsonNode;
    }

    @Override
    public Coin getCoinById(String coinId) {
        Optional<Coin> optionalCoin = coinRepository.findByCoinId(coinId);

        if (optionalCoin.isEmpty()) {
//            throw new APIException("Coin with id " + coinId + " not found in database");
            String url = baseUrl + "/coins/" + coinId;
            JsonNode jsonNode = fetchJsonResponse(url, "Failed to fetch coin details for coin: " + coinId);
            Coin coin = mapCoinDetails(jsonNode);
            return coinRepository.save(coin);
        }

        return optionalCoin.get();
    }

    @Override
    public JsonNode searchCoins(String query) {
        String url = baseUrl + "/search?query={query}";
        return fetchJsonResponse(url, "Failed to search coins for query: " + query, query);
    }

    @Override
    public JsonNode getTop50CoinsByMarketCapRank() {
        String url = baseUrl + "/coins/markets"
                + "?vs_currency=usd"
                + "&order=market_cap_desc"
                + "&per_page=50"
                + "&page=1"
                + "&sparkline=false";

        return fetchJsonResponse(url, "Failed to fetch top 50 coins by market cap rank");
    }

    @Override
    public JsonNode getTrendingCoins() {
        String url = baseUrl + "/coins/markets"
                + "?vs_currency=usd"
                + "&order=volume_desc"
                + "&per_page=10"
                + "&page=1"
                + "&sparkline=false";

        return fetchJsonResponse(url, "Failed to fetch trending coins");
    }

    private CoinGeckoMarketResponseDTO[] fetchAndParseArray(String url) {
        externalApiRateLimiter.acquireCoinGecko();
        try {
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isBlank()) {
                throw new APIException("Failed to fetch data from CoinGecko");
            }

            return objectMapper.readValue(response, CoinGeckoMarketResponseDTO[].class);
        } catch (RestClientException | JsonProcessingException e) {
            throw new APIException("Failed to parse CoinGecko response");
        }
    }

    private JsonNode fetchJsonResponse(String url, String errorMessage, Object... uriVariables) {
        externalApiRateLimiter.acquireCoinGecko();
        try {
            String response = restTemplate.getForObject(url, String.class, uriVariables);

            if (response == null || response.isBlank()) {
                throw new APIException(errorMessage);
            }

            return objectMapper.readTree(response);
        } catch (RestClientException | JsonProcessingException e) {
            throw new APIException(errorMessage);
        }
    }

    private Coin mapToCoinEntity(CoinGeckoMarketResponseDTO dto) {
        Coin coin = new Coin();

        coin.setCoinId(dto.getId());
        coin.setSymbol(dto.getSymbol());
        coin.setName(dto.getName());
        coin.setImage(dto.getImage());
        coin.setCurrentPrice(dto.getCurrentPrice());
        coin.setMarketCap(dto.getMarketCap());
        coin.setMarketCapRank(dto.getMarketCapRank());
        coin.setCirculatingSupply(dto.getCirculatingSupply());
        coin.setTotalSupply(dto.getTotalSupply());
        coin.setMaxSupply(dto.getMaxSupply());
        coin.setAth(dto.getAth());
        coin.setAtl(dto.getAtl());
        coin.setLastUpdated(dto.getLastUpdated());

        return coin;
    }

    private Coin mapToCoin(JsonNode root) {
        Coin coin = new Coin();

        coin.setCoinId(root.get("id").asText());
        coin.setSymbol(root.get("symbol").asText());
        coin.setName(root.get("name").asText());
        coin.setImage(root.get("image").asText());
        coin.setCurrentPrice(root.get("current_price").asDouble());
        coin.setMarketCap(root.get("market_cap").asLong());
        coin.setMarketCapRank(root.get("market_cap_rank").asInt());
        coin.setCirculatingSupply(root.get("circulating_supply").asDouble());
        coin.setTotalSupply(root.get("total_supply").asDouble());
        coin.setMaxSupply(root.get("max_supply").asDouble());
        coin.setAth(root.get("ath").asDouble());
        coin.setAtl(root.get("atl").asDouble());
        coin.setLastUpdated(root.get("last_updated").asText());

        return coin;
    }

    private Coin mapCoinDetails(JsonNode root) {
        Coin coin = new Coin();

        coin.setCoinId(root.get("id").asText());
        coin.setSymbol(root.get("symbol").asText());
        coin.setName(root.get("name").asText());

        coin.setImage(root.get("image").get("large").asText());

        JsonNode marketData = root.get("market_data");

        coin.setCurrentPrice(
                marketData.get("current_price").get("usd").asDouble()
        );

        coin.setMarketCap(
                marketData.get("market_cap").get("usd").asLong()
        );

        coin.setMarketCapRank(root.get("market_cap_rank").asInt());

        coin.setCirculatingSupply(
                marketData.get("circulating_supply").asDouble()
        );

        coin.setTotalSupply(
                marketData.get("total_supply").asDouble()
        );

        coin.setMaxSupply(
                marketData.get("max_supply").asDouble()
        );

        coin.setAth(
                marketData.get("ath").get("usd").asDouble()
        );

        coin.setAtl(
                marketData.get("atl").get("usd").asDouble()
        );

        coin.setLastUpdated(root.get("last_updated").asText());

        return coin;
    }
}