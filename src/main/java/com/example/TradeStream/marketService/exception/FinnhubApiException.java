package com.example.TradeStream.marketService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class FinnhubApiException  extends RuntimeException {
    public FinnhubApiException(String message) {
        super(message);
    }
}
