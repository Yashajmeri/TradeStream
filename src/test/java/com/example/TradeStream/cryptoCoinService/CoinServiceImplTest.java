package com.example.TradeStream.cryptoCoinService;

import com.example.TradeStream.common.ExternalApiRateLimiter;
import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.cryptoCoinService.exception.APIException;
import com.example.TradeStream.cryptoCoinService.repositories.CoinRepository;
import com.example.TradeStream.cryptoCoinService.service.CoinServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoinServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CoinRepository coinRepository;

    @Mock
    private ExternalApiRateLimiter externalApiRateLimiter;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CoinServiceImpl coinService;

    private static final String COIN_DETAILS_JSON = """
            {
              "id": "bitcoin",
              "symbol": "btc",
              "name": "Bitcoin",
              "image": {"large": "https://example.com/btc.png"},
              "market_cap_rank": 1,
              "last_updated": "2024-01-01T00:00:00.000Z",
              "market_data": {
                "current_price": {"usd": 50000.0},
                "market_cap":    {"usd": 1000000000000},
                "circulating_supply": 19000000.0,
                "total_supply":  21000000.0,
                "max_supply":    21000000.0,
                "ath": {"usd": 69000.0},
                "atl": {"usd": 67.81}
              }
            }
            """;

    private static final String MARKET_LIST_JSON = """
            [
              {
                "id": "bitcoin",
                "symbol": "btc",
                "name": "Bitcoin",
                "image": "https://example.com/btc.png",
                "current_price": 50000.0,
                "market_cap": 1000000000000,
                "market_cap_rank": 1,
                "circulating_supply": 19000000.0,
                "total_supply": 21000000.0,
                "max_supply": 21000000.0,
                "ath": 69000.0,
                "atl": 67.81,
                "last_updated": "2024-01-01T00:00:00.000Z"
              }
            ]
            """;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(coinService, "baseUrl", "https://api.coingecko.com/api/v3");
    }

    // --- getCoinById ---

    @Test
    void getCoinById_whenFoundInDatabase_returnsDirectlyWithoutApiCall() {
        Coin coin = new Coin();
        coin.setCoinId("bitcoin");
        coin.setName("Bitcoin");
        when(coinRepository.findByCoinId("bitcoin")).thenReturn(Optional.of(coin));

        Coin result = coinService.getCoinById("bitcoin");

        assertThat(result.getCoinId()).isEqualTo("bitcoin");
        verify(restTemplate, never()).getForObject(anyString(), any(), any(Object[].class));
    }

    @Test
    void getCoinById_whenNotInDatabase_fetchesFromApiAndSaves() {
        when(coinRepository.findByCoinId("bitcoin")).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(COIN_DETAILS_JSON);
        Coin savedCoin = new Coin();
        savedCoin.setCoinId("bitcoin");
        when(coinRepository.save(any(Coin.class))).thenReturn(savedCoin);

        Coin result = coinService.getCoinById("bitcoin");

        assertThat(result.getCoinId()).isEqualTo("bitcoin");
        verify(coinRepository).save(any(Coin.class));
    }

    @Test
    void getCoinById_apiReturnsNull_throwsAPIException() {
        when(coinRepository.findByCoinId("badcoin")).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

        assertThatThrownBy(() -> coinService.getCoinById("badcoin"))
                .isInstanceOf(APIException.class);
    }

    // --- getAllCoins ---

    @Test
    void getAllCoins_normalResponse_returnsMappedCoinList() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(MARKET_LIST_JSON);

        List<Coin> result = coinService.getAllCoins(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCoinId()).isEqualTo("bitcoin");
        assertThat(result.get(0).getName()).isEqualTo("Bitcoin");
        assertThat(result.get(0).getCurrentPrice()).isEqualTo(50000.0);
    }

    @Test
    void getAllCoins_emptyArrayResponse_throwsAPIException() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("[]");

        assertThatThrownBy(() -> coinService.getAllCoins(1))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("No coins");
    }

    @Test
    void getAllCoins_nullResponse_throwsAPIException() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

        assertThatThrownBy(() -> coinService.getAllCoins(1))
                .isInstanceOf(APIException.class);
    }

    // --- getMarketChart ---

    @Test
    void getMarketChart_returnsJsonNodeWithPrices() {
        String json = "{\"prices\": [[1704067200000, 50000.0]]}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        JsonNode result = coinService.getMarketChart("bitcoin", 7);

        assertThat(result).isNotNull();
        assertThat(result.has("prices")).isTrue();
    }

    // --- searchCoins ---

    @Test
    void searchCoins_returnsJsonNodeFromApi() {
        String json = "{\"coins\": [{\"id\": \"ethereum\", \"name\": \"Ethereum\"}]}";
        when(restTemplate.getForObject(anyString(), eq(String.class), eq("eth"))).thenReturn(json);

        JsonNode result = coinService.searchCoins("eth");

        assertThat(result).isNotNull();
        assertThat(result.has("coins")).isTrue();
    }

    // --- getTop50CoinsByMarketCapRank ---

    @Test
    void getTop50CoinsByMarketCapRank_returnsJsonNode() {
        String json = "[{\"id\":\"bitcoin\"}]";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        JsonNode result = coinService.getTop50CoinsByMarketCapRank();

        assertThat(result).isNotNull();
        assertThat(result.isArray()).isTrue();
    }

    // --- getTrendingCoins ---

    @Test
    void getTrendingCoins_returnsJsonNode() {
        String json = "[{\"id\":\"solana\"}]";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        JsonNode result = coinService.getTrendingCoins();

        assertThat(result).isNotNull();
        assertThat(result.isArray()).isTrue();
    }
}
