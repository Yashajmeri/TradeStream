package com.example.TradeStream.marketService.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "candles",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"symbol", "resolution", "timestamp"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long candleId;

    @NotBlank(message = "Symbol is required for a candle")
    @Size(max = 20, message = "Symbol must not exceed 20 characters")
    @Column(nullable = false)
    private String symbol;       // "AAPL"

    @NotBlank(message = "Resolution is required for a candle")
    @Size(max = 10, message = "Resolution must not exceed 10 characters")
    @Column(nullable = false)
    private String resolution;   // "1", "5", "D" etc.

    @PositiveOrZero(message = "Open price cannot be negative")
    private Double open;

    @PositiveOrZero(message = "High price cannot be negative")
    private Double high;

    @PositiveOrZero(message = "Low price cannot be negative")
    private Double low;

    @PositiveOrZero(message = "Close price cannot be negative")
    private Double close;

    @PositiveOrZero(message = "Volume cannot be negative")
    private Long volume;

    @NotNull(message = "Timestamp is required for a candle")
    @Column(nullable = false)
    private Long timestamp;      // unix seconds

    private LocalDateTime candleTime; // human readable
}