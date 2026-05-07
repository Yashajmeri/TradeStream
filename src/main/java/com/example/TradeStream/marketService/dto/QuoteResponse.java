package com.example.TradeStream.marketService.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuoteResponse {
    private String symbol;
    private Double currentPrice;   // "c" from Finnhub
    private Double change;         // "d"
    private Double changePercent;  // "dp"
    private Double high;           // "h"
    private Double low;            // "l"
    private Double open;           // "o"
    private Double previousClose;  // "pc"
    private Long timestamp;        // "t"
}
