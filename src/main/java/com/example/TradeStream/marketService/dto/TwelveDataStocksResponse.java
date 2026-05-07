package com.example.TradeStream.marketService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwelveDataStocksResponse {

    private List<TwelveDataStockItem> data;
    private String status;
    private Integer code;
    private String message;
}