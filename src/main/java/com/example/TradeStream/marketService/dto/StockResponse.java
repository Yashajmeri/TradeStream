    package com.example.TradeStream.marketService.dto;

    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public class StockResponse {
        private Long id;
        private String symbol;
        private String name;
        private String exchange;
        private String currency;
        private String micCode;
//        private String type;
    }
