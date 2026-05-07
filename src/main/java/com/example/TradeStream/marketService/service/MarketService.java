package com.example.TradeStream.marketService.service;

import com.example.TradeStream.marketService.dto.CandleResponse;
import com.example.TradeStream.marketService.dto.MarketStatusResponse;
import com.example.TradeStream.marketService.dto.QuoteResponse;
import com.example.TradeStream.marketService.dto.StockResponse;
import com.example.TradeStream.marketService.dto.TwelveDataCompanyProfileResponse;
import com.example.TradeStream.marketService.dto.TwelveDataQuoteResponse;
import com.example.TradeStream.marketService.dto.TwelveDataStockItem;
import com.example.TradeStream.marketService.dto.TwelveDataStocksResponse;
import com.example.TradeStream.marketService.dto.TwelveDataStockSymbolResponse;
import com.example.TradeStream.marketService.dto.TwelveDataSymbolSearchResponse;
import com.example.TradeStream.marketService.dto.TwelveDataTimeSeriesResponse;
import com.example.TradeStream.marketService.entity.Stock;
import com.example.TradeStream.marketService.exception.MarketDataApiException;
import com.example.TradeStream.marketService.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketService {

    private static final String DEFAULT_EXCHANGE = "US";
    private static final String DEFAULT_TIMEZONE = "America/New_York";
    private static final String DEFAULT_MARKET_STATUS_SYMBOL = "AAPL";
    private static final int DEFAULT_STOCK_LOAD_LIMIT = 50;

    private static final DateTimeFormatter TWELVE_DATA_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter TWELVE_DATA_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StockRepository stockRepository;
    private final TwelveDataClient twelveDataClient;

    public List<StockResponse> getAllStocks() {
        List<Stock> stocks = stockRepository.findAll();

        if (stocks.isEmpty()) {
            loadStocksFromApi(DEFAULT_STOCK_LOAD_LIMIT);
            stocks = stockRepository.findAll();
        }

        return stocks.stream()
                .sorted(Comparator.comparing(Stock::getSymbol))
                .map(this::toStockResponse)
                .toList();
    }

    public StockResponse getStock(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);

        Stock stock = stockRepository.findBySymbol(normalizedSymbol)
                .orElseGet(() -> fetchAndSaveStock(normalizedSymbol));

        return toStockResponse(stock);
    }

    public List<StockResponse> searchStocks(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be null or blank");
        }

        String normalizedQuery = query.trim();

        List<Stock> localMatches = stockRepository
                .findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(normalizedQuery, normalizedQuery);

        if (!localMatches.isEmpty()) {
            return localMatches.stream()
                    .sorted(Comparator.comparing(Stock::getSymbol))
                    .map(this::toStockResponse)
                    .toList();
        }

        if (stockRepository.count() == 0) {
            loadStocksFromApi(DEFAULT_STOCK_LOAD_LIMIT);

            localMatches = stockRepository
                    .findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(normalizedQuery, normalizedQuery);

            if (!localMatches.isEmpty()) {
                return localMatches.stream()
                        .sorted(Comparator.comparing(Stock::getSymbol))
                        .map(this::toStockResponse)
                        .toList();
            }
        }

        TwelveDataSymbolSearchResponse response = twelveDataClient.searchSymbols(normalizedQuery);

        if (response.getData() == null || response.getData().isEmpty()) {
            return List.of();
        }

        List<StockResponse> results = new ArrayList<>();

        for (TwelveDataStockSymbolResponse item : response.getData()) {
            if (item.getSymbol() == null || item.getSymbol().isBlank()) {
                continue;
            }

            Stock savedStock = saveSearchResultIfNotExists(item);
            results.add(toStockResponse(savedStock));
        }

        return results.stream()
                .sorted(Comparator.comparing(StockResponse::getSymbol))
                .toList();
    }

    public QuoteResponse getQuote(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        TwelveDataQuoteResponse raw = twelveDataClient.getQuote(normalizedSymbol);

        return QuoteResponse.builder()
                .symbol(normalizedSymbol)
                .currentPrice(parseDouble(raw.getClose()))
                .change(parseDouble(raw.getChange()))
                .changePercent(parseDouble(raw.getPercentChange()))
                .high(parseDouble(raw.getHigh()))
                .low(parseDouble(raw.getLow()))
                .open(parseDouble(raw.getOpen()))
                .previousClose(parseDouble(raw.getPreviousClose()))
                .timestamp(resolveQuoteTimestamp(raw))
                .build();
    }

    public CandleResponse getCandles(String symbol, String resolution, long from, long to) {
        String normalizedSymbol = normalizeSymbol(symbol);

        if (from >= to) {
            throw new IllegalArgumentException("'from' must be less than 'to'");
        }

        String interval = mapResolutionToInterval(resolution);
        String startDate = formatUnixTimestamp(from);
        String endDate = formatUnixTimestamp(to);

        TwelveDataTimeSeriesResponse raw =
                twelveDataClient.getTimeSeries(normalizedSymbol, interval, startDate, endDate);

        if (raw.getValues() == null || raw.getValues().isEmpty()) {
            return CandleResponse.builder()
                    .symbol(normalizedSymbol)
                    .resolution(interval)
                    .candles(List.of())
                    .build();
        }

        validateCandleResponse(raw);

        String timezone = raw.getMeta() != null && raw.getMeta().getExchangeTimezone() != null
                ? raw.getMeta().getExchangeTimezone()
                : DEFAULT_TIMEZONE;

        List<CandleResponse.CandleData> candleDataList = new ArrayList<>();

        for (TwelveDataTimeSeriesResponse.Value value : raw.getValues()) {
            long ts = parseDateTimeToEpoch(value.getDatetime(), timezone);

            candleDataList.add(
                    CandleResponse.CandleData.builder()
                            .open(parseDouble(value.getOpen()))
                            .high(parseDouble(value.getHigh()))
                            .low(parseDouble(value.getLow()))
                            .close(parseDouble(value.getClose()))
                            .volume(parseLong(value.getVolume()))
                            .timestamp(ts)
                            .time(LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC)
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                            .build()
            );
        }

        return CandleResponse.builder()
                .symbol(normalizedSymbol)
                .resolution(interval)
                .candles(candleDataList)
                .build();
    }

    public MarketStatusResponse getMarketStatus() {
        TwelveDataQuoteResponse raw = twelveDataClient.getQuote(DEFAULT_MARKET_STATUS_SYMBOL);
        boolean isOpen = Boolean.TRUE.equals(raw.getIsMarketOpen());

        return MarketStatusResponse.builder()
                .exchange(raw.getExchange() != null ? raw.getExchange() : DEFAULT_EXCHANGE)
                .isOpen(isOpen)
                .session(isOpen ? "regular" : "closed")
                .timezone(DEFAULT_TIMEZONE)
                .message(isOpen ? "Market is open" : "Market is closed")
                .build();
    }

    public StockResponse addStock(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);

        if (stockRepository.existsBySymbol(normalizedSymbol)) {
            return getStock(normalizedSymbol);
        }

        return toStockResponse(fetchAndSaveStock(normalizedSymbol));
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol must not be null or blank");
        }
        return symbol.trim().toUpperCase();
    }

    private void loadStocksFromApi(int limit) {
        TwelveDataStocksResponse response = twelveDataClient.getAllStocks(limit);

        if (response.getData() == null || response.getData().isEmpty()) {
            throw new MarketDataApiException("No stocks returned from Twelve Data API");
        }

        List<TwelveDataStockItem> items = response.getData();
        if (items.size() > limit) {
            items = items.subList(0, limit);
        }

        for (TwelveDataStockItem item : items) {
            if (item.getSymbol() == null || item.getSymbol().isBlank()) {
                continue;
            }

            String normalizedSymbol = normalizeSymbol(item.getSymbol());

            if (stockRepository.existsBySymbol(normalizedSymbol)) {
                continue;
            }

            Stock stock = Stock.builder()
                    .symbol(normalizedSymbol)
                    .name(item.getName())
                    .exchange(item.getExchange())
                    .currency(item.getCurrency() != null ? item.getCurrency() : "USD")
                    .micCode(item.getMicCode())
                    .build();

            stockRepository.save(stock);
        }
    }

    private Stock fetchAndSaveStock(String symbol) {
        TwelveDataCompanyProfileResponse profile = twelveDataClient.getCompanyProfile(symbol);

        if (profile.getName() == null || profile.getName().isBlank()) {
            throw new MarketDataApiException("No valid company profile found for symbol: " + symbol);
        }

        Stock existing = stockRepository.findBySymbol(symbol).orElse(null);

        if (existing != null) {
            existing.setName(profile.getName());
            existing.setExchange(profile.getExchange());
            existing.setCurrency(profile.getCurrency() != null ? profile.getCurrency() : "USD");
           existing.setMicCode(profile.getMicCode());
            return stockRepository.save(existing);
        }

        Stock stock = Stock.builder()
                .symbol(symbol)
                .name(profile.getName())
                .exchange(profile.getExchange())
                .currency(profile.getCurrency() != null ? profile.getCurrency() : "USD")
                .micCode(profile.getMicCode())
                .type(profile.getType())
                .build();

        return stockRepository.save(stock);
    }

    private Stock saveSearchResultIfNotExists(TwelveDataStockSymbolResponse item) {
        String normalizedSymbol = normalizeSymbol(item.getSymbol());

        return stockRepository.findBySymbol(normalizedSymbol)
                .orElseGet(() -> stockRepository.save(
                        Stock.builder()
                                .symbol(normalizedSymbol)
                                .name(item.getInstrumentName())
                                .exchange(item.getExchange())
                                .currency(item.getCurrency() != null ? item.getCurrency() : "USD")
                                .type(item.getInstrumentType())//for now
                                .build()
                ));
    }

    private StockResponse toStockResponse(Stock stock) {
        return StockResponse.builder()
                .id(stock.getStockId())
                .symbol(stock.getSymbol())
                .name(stock.getName())
                .exchange(stock.getExchange())
                .currency(stock.getCurrency())
                .micCode(stock.getMicCode())
//                .type(stock.getType())
                .build();
    }

    private void validateCandleResponse(TwelveDataTimeSeriesResponse raw) {
        for (TwelveDataTimeSeriesResponse.Value value : raw.getValues()) {
            if (value.getOpen() == null ||
                    value.getHigh() == null ||
                    value.getLow() == null ||
                    value.getClose() == null ||
                    value.getDatetime() == null) {
                throw new MarketDataApiException("Incomplete candle data returned by Twelve Data");
            }
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }

    private String mapResolutionToInterval(String resolution) {
        return switch (resolution.toUpperCase()) {
            case "1", "1M" -> "1min";
            case "5", "5M" -> "5min";
            case "15", "15M" -> "15min";
            case "30", "30M" -> "30min";
            case "60", "1H" -> "1h";
            case "240", "4H" -> "4h";
            case "W" -> "1week";
            case "M" -> "1month";
            case "D" -> "1day";
            default -> throw new IllegalArgumentException("Unsupported resolution: " + resolution);
        };
    }

    private String formatUnixTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC)
                .format(TWELVE_DATA_DATE_TIME);
    }

    private long parseDateTimeToEpoch(String dateTime, String timezone) {
        LocalDateTime parsed = parseTwelveDataDateTime(dateTime);
        return parsed.atZone(ZoneId.of(timezone)).toEpochSecond();
    }

    private LocalDateTime parseTwelveDataDateTime(String dateTime) {
        try {
            return LocalDateTime.parse(dateTime, TWELVE_DATA_DATE_TIME);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(dateTime, TWELVE_DATA_DATE).atStartOfDay();
            } catch (DateTimeParseException innerEx) {
                throw new MarketDataApiException("Invalid datetime returned by Twelve Data: " + dateTime);
            }
        }
    }

    private Long resolveQuoteTimestamp(TwelveDataQuoteResponse raw) {
        if (raw.getTimestamp() != null) {
            return raw.getTimestamp();
        }

        if (raw.getDatetime() != null && !raw.getDatetime().isBlank()) {
            return parseDateTimeToEpoch(raw.getDatetime(), DEFAULT_TIMEZONE);
        }

        return null;
    }
}