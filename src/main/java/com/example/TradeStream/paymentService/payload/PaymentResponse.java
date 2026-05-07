package com.example.TradeStream.paymentService.payload;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
    public class PaymentResponse {
        private String paymentURL;
        private Long paymentOrderId;
    }
