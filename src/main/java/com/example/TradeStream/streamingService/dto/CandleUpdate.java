package com.example.TradeStream.streamingService.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandleUpdate {
    private String symbol;
    private String interval;
    private long openTime;
    private long closeTime;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
    private boolean isClosed;
}
