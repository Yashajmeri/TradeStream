package com.example.TradeStream.marketService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwelveDataStockSymbolResponse {

    private String symbol;

    @JsonProperty("instrument_name")
    private String instrumentName;

    private String exchange;
    private String country;
    private String currency;

    @JsonProperty("instrument_type")
    private String instrumentType;
}
