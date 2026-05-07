package com.example.TradeStream.paymentService.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentDetailsRequest {

    @NotBlank(message = "Account number is required")
    @Size(max = 34, message = "Account number must not exceed 34 characters (IBAN format)")
    private String accountNumber;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @NotBlank(message = "IFSC code (or routing code) is required")
    @Size(max = 20, message = "IFSC/routing code must not exceed 20 characters")
    private String ifscCode;

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    private String accountHolderName;
}
