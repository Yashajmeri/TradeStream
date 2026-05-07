package com.example.TradeStream.marketService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwelveDataStockItem {

    private String symbol;
    private String name;
    private String exchange;

    @JsonProperty("mic_code")
    private String micCode;

    private String country;
    private String type;
    private String currency;
}