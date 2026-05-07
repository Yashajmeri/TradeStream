package com.example.TradeStream.marketService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwelveDataCompanyProfileResponse {

    private String symbol;
    private String name;
    private String exchange;
    private String currency;
    private String country;
    private String type;
    private String industry;
    private String sector;
    private String website;
    private String description;
    private String logo;
    private String micCode;
    @JsonProperty("market_cap")
    private Double marketCap;
}
