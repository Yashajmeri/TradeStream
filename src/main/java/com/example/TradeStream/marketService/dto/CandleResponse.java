package com.example.TradeStream.marketService.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CandleResponse {
    private String symbol;
    private String resolution;
    private List<CandleData> candles;

    @Data @Builder
    public static class CandleData {
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Long volume;
        private Long timestamp;
        private String time; // human readable e.g. "2024-03-06 10:00"
    }
}