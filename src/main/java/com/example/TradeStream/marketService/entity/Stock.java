package com.example.TradeStream.marketService.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockId;

    @NotBlank(message = "Stock symbol is required")
    @Size(max = 20, message = "Symbol must not exceed 20 characters")
    @Column(unique = true, nullable = false)
    private String symbol;       // "AAPL"

    @NotBlank(message = "Stock name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    @Column(nullable = false)
    private String name;         // "Apple Inc."

    @Size(max = 50, message = "Exchange must not exceed 50 characters")
    private String exchange;     // "NASDAQ"

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;     // "USD"

    @Size(max = 20, message = "MIC code must not exceed 20 characters")
    private String micCode;

    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @CreationTimestamp
    private LocalDateTime createdAt;
}