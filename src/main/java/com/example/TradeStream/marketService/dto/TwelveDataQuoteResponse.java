package com.example.TradeStream.marketService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwelveDataQuoteResponse {

    private String symbol;
    private String name;
    private String exchange;
    private String currency;
    private String datetime;
    private Long timestamp;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;

    @JsonProperty("previous_close")
    private String previousClose;

    private String change;

    @JsonProperty("percent_change")
    private String percentChange;

    @JsonProperty("is_market_open")
    private Boolean isMarketOpen;
}
