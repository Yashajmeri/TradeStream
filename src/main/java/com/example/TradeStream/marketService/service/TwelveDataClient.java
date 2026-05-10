package com.example.TradeStream.marketService.service;

import com.example.TradeStream.common.ExternalApiRateLimiter;
import com.example.TradeStream.marketService.dto.*;
import com.example.TradeStream.marketService.exception.MarketDataApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwelveDataClient {

    @Value("${twelvedata.api.key}")
    private String apiKey;

    @Value("${twelvedata.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ExternalApiRateLimiter externalApiRateLimiter;

    public TwelveDataStocksResponse getAllStocks(int limit) {
        externalApiRateLimiter.acquireTwelveData();
        String url = baseUrl + "/stocks?country=United States&type=Common Stock&apikey=" + apiKey;

        try {
            TwelveDataStocksResponse response =
                    restTemplate.getForObject(url, TwelveDataStocksResponse.class);

            if (response == null) {
                throw new MarketDataApiException("Null response from Twelve Data stocks API");
            }

            if (response.getStatus() != null && "error".equalsIgnoreCase(response.getStatus())) {
                throw new MarketDataApiException(
                        response.getMessage() != null
                                ? response.getMessage()
                                : "Failed to fetch stocks from Twelve Data API"
                );
            }


            if (response.getData() != null && response.getData().size() > limit) {
                response.setData(response.getData().subList(0, limit));
            }
            log.debug("Fetched {} stocks from Twelve Data", response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (RestClientException ex) {
            log.error("Twelve Data stocks API failed with limit {}", limit, ex);
            throw new MarketDataApiException("Failed to fetch stocks from Twelve Data API", ex);
        }
    }

    public TwelveDataQuoteResponse getQuote(String symbol) {
        externalApiRateLimiter.acquireTwelveData();
        String url = baseUrl + "/quote?symbol=" + symbol + "&apikey=" + apiKey;

        try {
            TwelveDataQuoteResponse response =
                    restTemplate.getForObject(url, TwelveDataQuoteResponse.class);

            if (response == null || response.getSymbol() == null || response.getSymbol().isBlank()) {
                throw new MarketDataApiException("Failed to fetch quote for symbol: " + symbol);
            }

            return response;
        } catch (RestClientException ex) {
            log.error("Twelve Data quote API failed for symbol {}", symbol, ex);
            throw new MarketDataApiException("Failed to fetch quote for symbol: " + symbol);
        }
    }

    public TwelveDataTimeSeriesResponse getTimeSeries(String symbol, String interval, String startDate, String endDate) {
        externalApiRateLimiter.acquireTwelveData();
        String url = baseUrl + "/time_series?symbol=" + symbol
                + "&interval=" + interval
                + "&start_date=" + startDate
                + "&end_date=" + endDate
                + "&orderby=ASC"
                + "&apikey=" + apiKey;

        try {
            TwelveDataTimeSeriesResponse response =
                    restTemplate.getForObject(url, TwelveDataTimeSeriesResponse.class);

            if (response == null) {
                throw new MarketDataApiException("Null response from Twelve Data time series API for symbol: " + symbol);
            }

            if (response.getStatus() != null && "error".equalsIgnoreCase(response.getStatus())) {
                throw new MarketDataApiException(
                        response.getMessage() != null
                                ? response.getMessage()
                                : "Failed to fetch candle data for symbol: " + symbol
                );
            }

            return response;
        } catch (RestClientException ex) {
            log.error("Twelve Data time series API failed for symbol {}, interval {}, start {}, end {}",
                    symbol, interval, startDate, endDate, ex);
            throw new MarketDataApiException("Failed to fetch candle data for symbol: " + symbol);
        }
    }

    public TwelveDataSymbolSearchResponse searchSymbols(String query) {
        externalApiRateLimiter.acquireTwelveData();
        String url = baseUrl + "/symbol_search?symbol=" + query + "&outputsize=20&apikey=" + apiKey;

        try {
            TwelveDataSymbolSearchResponse response =
                    restTemplate.getForObject(url, TwelveDataSymbolSearchResponse.class);

            if (response == null) {
                throw new MarketDataApiException("Null response from Twelve Data symbol search API for query: " + query);
            }

            if (response.getStatus() != null && "error".equalsIgnoreCase(response.getStatus())) {
                throw new MarketDataApiException(
                        response.getMessage() != null
                                ? response.getMessage()
                                : "Failed to search stocks for query: " + query
                );
            }

            return response;
        } catch (RestClientException ex) {
            log.error("Twelve Data symbol search API failed for query {}", query, ex);
            throw new MarketDataApiException("Failed to search stocks for query: " + query);
        }
    }

    public TwelveDataCompanyProfileResponse getCompanyProfile(String symbol) {
        externalApiRateLimiter.acquireTwelveData();
        String url = baseUrl + "/profile?symbol=" + symbol + "&apikey=" + apiKey;

        try {
            TwelveDataCompanyProfileResponse response =
                    restTemplate.getForObject(url, TwelveDataCompanyProfileResponse.class);

            if (response == null || response.getName() == null || response.getName().isBlank()) {
                throw new MarketDataApiException("No valid company profile found for symbol: " + symbol);
            }
            log.debug("Fetched company profile for symbol {}", symbol);
            return response;
        } catch (RestClientException ex) {
            log.error("Twelve Data profile API failed for symbol {}", symbol, ex);
            throw new MarketDataApiException("Failed to fetch company profile for symbol: " + symbol);
        }
    }
}