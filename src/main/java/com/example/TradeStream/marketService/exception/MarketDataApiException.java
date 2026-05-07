package com.example.TradeStream.marketService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class MarketDataApiException extends RuntimeException {
    public MarketDataApiException(String message) {
        super(message);
    }

    public MarketDataApiException(String message, Throwable cause) {
        super(message, cause);
    }
}