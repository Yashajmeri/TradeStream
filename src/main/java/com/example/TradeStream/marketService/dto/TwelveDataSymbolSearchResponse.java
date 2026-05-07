package com.example.TradeStream.marketService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwelveDataSymbolSearchResponse {

    private List<TwelveDataStockSymbolResponse> data;
    private String status;
    private String code;
    private String message;
}
