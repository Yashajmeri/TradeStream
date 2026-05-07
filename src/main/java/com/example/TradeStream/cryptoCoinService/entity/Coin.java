package com.example.TradeStream.cryptoCoinService.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "coins")
public class Coin {

    @Id
    @NotBlank(message = "Coin ID is required")
    @Size(max = 100, message = "Coin ID must not exceed 100 characters")
    private String coinId;   // bitcoin, ethereum

    @Size(max = 20, message = "Symbol must not exceed 20 characters")
    private String symbol;

    @Size(max = 200, message = "Coin name must not exceed 200 characters")
    private String name;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String image;

    private Double currentPrice;
    private Long marketCap;
    private Integer marketCapRank;

    private Double circulatingSupply;
    private Double totalSupply;
    private Double maxSupply;

    private Double ath;
    private Double atl;

    @Size(max = 100, message = "Last updated field must not exceed 100 characters")
    private String lastUpdated;
}