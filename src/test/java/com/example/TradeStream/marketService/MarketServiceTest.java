package com.example.TradeStream.marketService;

import com.example.TradeStream.marketService.dto.*;
import com.example.TradeStream.marketService.entity.Stock;
import com.example.TradeStream.marketService.exception.MarketDataApiException;
import com.example.TradeStream.marketService.repositories.StockRepository;
import com.example.TradeStream.marketService.service.MarketService;
import com.example.TradeStream.marketService.service.TwelveDataClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private TwelveDataClient twelveDataClient;

    @InjectMocks
    private MarketService marketService;

    private Stock appleStock;

    @BeforeEach
    void setUp() {
        appleStock = Stock.builder()
                .stockId(1L)
                .symbol("AAPL")
                .name("Apple Inc.")
                .exchange("NASDAQ")
                .currency("USD")
                .micCode("XNAS")
                .build();
    }

    // --- getAllStocks ---

    @Test
    void getAllStocks_whenStocksExistInDb_returnsFromDbWithoutApiCall() {
        when(stockRepository.findAll()).thenReturn(List.of(appleStock));

        List<StockResponse> result = marketService.getAllStocks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
        verify(twelveDataClient, never()).getAllStocks(anyInt());
    }

    @Test
    void getAllStocks_whenDbIsEmpty_loadsFromApiAndReturns() {
        TwelveDataStockItem item = new TwelveDataStockItem();
        item.setSymbol("TSLA");
        item.setName("Tesla Inc.");
        item.setExchange("NASDAQ");
        item.setCurrency("USD");

        TwelveDataStocksResponse apiResponse = new TwelveDataStocksResponse();
        apiResponse.setData(List.of(item));

        Stock savedStock = Stock.builder()
                .stockId(2L).symbol("TSLA").name("Tesla Inc.")
                .exchange("NASDAQ").currency("USD").build();

        when(stockRepository.findAll())
                .thenReturn(List.of())
                .thenReturn(List.of(savedStock));
        when(twelveDataClient.getAllStocks(50)).thenReturn(apiResponse);
        when(stockRepository.existsBySymbol("TSLA")).thenReturn(false);
        when(stockRepository.save(any(Stock.class))).thenReturn(savedStock);

        List<StockResponse> result = marketService.getAllStocks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("TSLA");
        verify(twelveDataClient).getAllStocks(50);
    }

    // --- getStock ---

    @Test
    void getStock_whenFoundInDb_returnsFromDbWithoutApiCall() {
        when(stockRepository.findBySymbol("AAPL")).thenReturn(Optional.of(appleStock));

        StockResponse result = marketService.getStock("aapl");

        assertThat(result.getSymbol()).isEqualTo("AAPL");
        verify(twelveDataClient, never()).getCompanyProfile(anyString());
    }

    @Test
    void getStock_whenNotInDb_fetchesProfileFromApi() {
        TwelveDataCompanyProfileResponse profile = new TwelveDataCompanyProfileResponse();
        profile.setName("Apple Inc.");
        profile.setExchange("NASDAQ");
        profile.setCurrency("USD");

        when(stockRepository.findBySymbol("AAPL")).thenReturn(Optional.empty());
        when(twelveDataClient.getCompanyProfile("AAPL")).thenReturn(profile);
        when(stockRepository.findBySymbol("AAPL")).thenReturn(Optional.empty());
        when(stockRepository.save(any(Stock.class))).thenReturn(appleStock);

        StockResponse result = marketService.getStock("aapl");

        assertThat(result.getSymbol()).isEqualTo("AAPL");
        verify(twelveDataClient).getCompanyProfile("AAPL");
    }

    // --- searchStocks ---

    @Test
    void searchStocks_nullQuery_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> marketService.searchStocks(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void searchStocks_blankQuery_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> marketService.searchStocks("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void searchStocks_whenLocalMatchFound_returnsLocalResultsWithoutApiCall() {
        when(stockRepository.findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase("apple", "apple"))
                .thenReturn(List.of(appleStock));

        List<StockResponse> result = marketService.searchStocks("apple");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Apple Inc.");
        verify(twelveDataClient, never()).searchSymbols(anyString());
    }

    @Test
    void searchStocks_whenNoLocalMatch_callsApiAndReturnsResults() {
        TwelveDataStockSymbolResponse symbolResponse = new TwelveDataStockSymbolResponse();
        symbolResponse.setSymbol("MSFT");
        symbolResponse.setInstrumentName("Microsoft Corporation");
        symbolResponse.setExchange("NASDAQ");
        symbolResponse.setCurrency("USD");

        TwelveDataSymbolSearchResponse searchResponse = new TwelveDataSymbolSearchResponse();
        searchResponse.setData(List.of(symbolResponse));

        Stock msftStock = Stock.builder()
                .stockId(3L).symbol("MSFT").name("Microsoft Corporation")
                .exchange("NASDAQ").currency("USD").build();

        when(stockRepository.findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase("msft", "msft"))
                .thenReturn(List.of());
        when(stockRepository.count()).thenReturn(10L);
        when(twelveDataClient.searchSymbols("msft")).thenReturn(searchResponse);
        when(stockRepository.findBySymbol("MSFT")).thenReturn(Optional.empty());
        when(stockRepository.save(any(Stock.class))).thenReturn(msftStock);

        List<StockResponse> result = marketService.searchStocks("msft");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("MSFT");
        verify(twelveDataClient).searchSymbols("msft");
    }

    @Test
    void searchStocks_whenApiReturnsEmpty_returnsEmptyList() {
        TwelveDataSymbolSearchResponse emptyResponse = new TwelveDataSymbolSearchResponse();
        emptyResponse.setData(List.of());

        when(stockRepository.findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase("xyz", "xyz"))
                .thenReturn(List.of());
        when(stockRepository.count()).thenReturn(5L);
        when(twelveDataClient.searchSymbols("xyz")).thenReturn(emptyResponse);

        List<StockResponse> result = marketService.searchStocks("xyz");

        assertThat(result).isEmpty();
    }

    // --- getQuote ---

    @Test
    void getQuote_mapsApiResponseToQuoteResponse() {
        TwelveDataQuoteResponse raw = new TwelveDataQuoteResponse();
        raw.setSymbol("AAPL");
        raw.setClose("175.50");
        raw.setChange("1.25");
        raw.setPercentChange("0.72");
        raw.setHigh("176.00");
        raw.setLow("174.00");
        raw.setOpen("174.50");
        raw.setPreviousClose("174.25");
        raw.setTimestamp(1704067200L);
        raw.setIsMarketOpen(true);

        when(twelveDataClient.getQuote("AAPL")).thenReturn(raw);

        QuoteResponse result = marketService.getQuote("aapl");

        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getCurrentPrice()).isEqualTo(175.50);
        assertThat(result.getChange()).isEqualTo(1.25);
        assertThat(result.getHigh()).isEqualTo(176.00);
        assertThat(result.getLow()).isEqualTo(174.00);
        assertThat(result.getTimestamp()).isEqualTo(1704067200L);
    }

    // --- getCandles ---

    @Test
    void getCandles_fromGreaterThanTo_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> marketService.getCandles("AAPL", "D", 1000L, 500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }

    @Test
    void getCandles_fromEqualToTo_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> marketService.getCandles("AAPL", "D", 1000L, 1000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getCandles_emptyValues_returnsEmptyCandleResponse() {
        TwelveDataTimeSeriesResponse raw = new TwelveDataTimeSeriesResponse();
        raw.setValues(List.of());

        when(twelveDataClient.getTimeSeries(eq("AAPL"), eq("1day"), anyString(), anyString()))
                .thenReturn(raw);

        CandleResponse result = marketService.getCandles("aapl", "D", 1000L, 2000L);

        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getCandles()).isEmpty();
    }

    @Test
    void getCandles_validData_returnsMappedCandleResponse() {
        TwelveDataTimeSeriesResponse.Value value = new TwelveDataTimeSeriesResponse.Value();
        value.setDatetime("2024-01-02 09:30:00");
        value.setOpen("174.00");
        value.setHigh("176.00");
        value.setLow("173.50");
        value.setClose("175.50");
        value.setVolume("5000000");

        TwelveDataTimeSeriesResponse.Meta meta = new TwelveDataTimeSeriesResponse.Meta();
        meta.setExchangeTimezone("America/New_York");

        TwelveDataTimeSeriesResponse raw = new TwelveDataTimeSeriesResponse();
        raw.setValues(List.of(value));
        raw.setMeta(meta);

        when(twelveDataClient.getTimeSeries(eq("AAPL"), eq("1day"), anyString(), anyString()))
                .thenReturn(raw);

        CandleResponse result = marketService.getCandles("aapl", "D", 1000L, 9999999L);

        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getCandles()).hasSize(1);
        CandleResponse.CandleData candle = result.getCandles().get(0);
        assertThat(candle.getOpen()).isEqualTo(174.00);
        assertThat(candle.getHigh()).isEqualTo(176.00);
        assertThat(candle.getClose()).isEqualTo(175.50);
        assertThat(candle.getVolume()).isEqualTo(5_000_000L);
    }

    @Test
    void getCandles_unsupportedResolution_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> marketService.getCandles("AAPL", "INVALID", 1000L, 9999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported resolution");
    }

    // --- getMarketStatus ---

    @Test
    void getMarketStatus_whenMarketIsOpen_returnsOpenStatus() {
        TwelveDataQuoteResponse raw = new TwelveDataQuoteResponse();
        raw.setExchange("NASDAQ");
        raw.setIsMarketOpen(true);
        when(twelveDataClient.getQuote("AAPL")).thenReturn(raw);

        MarketStatusResponse result = marketService.getMarketStatus();

        assertThat(result.getIsOpen()).isTrue();
        assertThat(result.getSession()).isEqualTo("regular");
        assertThat(result.getMessage()).contains("open");
    }

    @Test
    void getMarketStatus_whenMarketIsClosed_returnsClosedStatus() {
        TwelveDataQuoteResponse raw = new TwelveDataQuoteResponse();
        raw.setExchange("NASDAQ");
        raw.setIsMarketOpen(false);
        when(twelveDataClient.getQuote("AAPL")).thenReturn(raw);

        MarketStatusResponse result = marketService.getMarketStatus();

        assertThat(result.getIsOpen()).isFalse();
        assertThat(result.getSession()).isEqualTo("closed");
        assertThat(result.getMessage()).contains("closed");
    }

    // --- addStock ---

    @Test
    void addStock_whenAlreadyExists_returnsExistingStockWithoutApiCall() {
        when(stockRepository.existsBySymbol("AAPL")).thenReturn(true);
        when(stockRepository.findBySymbol("AAPL")).thenReturn(Optional.of(appleStock));

        StockResponse result = marketService.addStock("aapl");

        assertThat(result.getSymbol()).isEqualTo("AAPL");
        verify(twelveDataClient, never()).getCompanyProfile(anyString());
    }

    @Test
    void addStock_whenNew_fetchesProfileAndSaves() {
        TwelveDataCompanyProfileResponse profile = new TwelveDataCompanyProfileResponse();
        profile.setName("Apple Inc.");
        profile.setExchange("NASDAQ");
        profile.setCurrency("USD");

        when(stockRepository.existsBySymbol("AAPL")).thenReturn(false);
        when(twelveDataClient.getCompanyProfile("AAPL")).thenReturn(profile);
        when(stockRepository.findBySymbol("AAPL")).thenReturn(Optional.empty());
        when(stockRepository.save(any(Stock.class))).thenReturn(appleStock);

        StockResponse result = marketService.addStock("aapl");

        assertThat(result.getSymbol()).isEqualTo("AAPL");
        verify(twelveDataClient).getCompanyProfile("AAPL");
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    void addStock_symbolNormalizesToUpperCase() {
        TwelveDataCompanyProfileResponse profile = new TwelveDataCompanyProfileResponse();
        profile.setName("Apple Inc.");
        profile.setExchange("NASDAQ");

        when(stockRepository.existsBySymbol("AAPL")).thenReturn(false);
        when(twelveDataClient.getCompanyProfile("AAPL")).thenReturn(profile);
        when(stockRepository.findBySymbol("AAPL")).thenReturn(Optional.empty());
        when(stockRepository.save(any(Stock.class))).thenReturn(appleStock);

        marketService.addStock("aapl");

        verify(stockRepository).existsBySymbol("AAPL");
    }
}
