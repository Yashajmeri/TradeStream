package com.example.TradeStream.marketService.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarketStatusResponse {
    private String exchange;
    private Boolean isOpen;
    private String session;    // "regular", "pre-market", "post-market"
    private String timezone;
    private String message;    // "Market is open" / "Market is closed"
}