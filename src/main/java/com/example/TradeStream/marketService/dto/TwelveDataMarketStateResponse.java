package com.example.TradeStream.marketService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwelveDataMarketStateResponse {

    private String symbol;
    private String exchange;

    @JsonProperty("is_market_open")
    private Boolean isMarketOpen;

    private String datetime;
}
