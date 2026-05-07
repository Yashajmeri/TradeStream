package com.example.TradeStream.assetService;

import com.example.TradeStream.assetService.entity.Asset;
import com.example.TradeStream.assetService.exception.ResourceNotFoundException;
import com.example.TradeStream.assetService.repository.AssetRepository;
import com.example.TradeStream.assetService.service.AssetServiceImpl;
import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.userService.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceImplTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetServiceImpl assetService;

    private User user;
    private Coin coin;
    private Asset asset;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("password123");

        coin = new Coin();
        coin.setCoinId("bitcoin");
        coin.setSymbol("BTC");
        coin.setName("Bitcoin");
        coin.setCurrentPrice(50000.0);

        asset = new Asset();
        asset.setId(1L);
        asset.setUser(user);
        asset.setCoin(coin);
        asset.setQuantity(new BigDecimal("2.0"));
        asset.setBuyPrice(new BigDecimal("50000.00"));
    }

    // --- createAsset ---

    @Test
    void createAsset_savesAssetWithCoinCurrentPriceAsBuyPrice() {
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        Asset result = assetService.createAsset(user, coin, new BigDecimal("0.5"));

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getCoin()).isEqualTo(coin);
        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(result.getBuyPrice()).isEqualByComparingTo(BigDecimal.valueOf(coin.getCurrentPrice()));
        verify(assetRepository).save(any(Asset.class));
    }

    // --- getAssetById ---

    @Test
    void getAssetById_whenFound_returnsAsset() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        Asset result = assetService.getAssetById(1L);

        assertThat(result).isEqualTo(asset);
    }

    @Test
    void getAssetById_whenNotFound_throwsResourceNotFoundException() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getAssetById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- getAssetByUserIdAndId ---

    @Test
    void getAssetByUserIdAndId_whenFound_returnsAsset() {
        when(assetRepository.findByUser_IdAndId(1L, 1L)).thenReturn(Optional.of(asset));

        Asset result = assetService.getAssetByUserIdAndId(1L, 1L);

        assertThat(result).isEqualTo(asset);
    }

    @Test
    void getAssetByUserIdAndId_whenNotFound_throwsResourceNotFoundException() {
        when(assetRepository.findByUser_IdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getAssetByUserIdAndId(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getAssetsByUserId ---

    @Test
    void getAssetsByUserId_returnsAllUserAssets() {
        Asset asset2 = new Asset();
        asset2.setId(2L);
        when(assetRepository.findByUser_Id(1L)).thenReturn(List.of(asset, asset2));

        List<Asset> result = assetService.getAssetsByUserId(1L);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(asset, asset2);
    }

    @Test
    void getAssetsByUserId_noAssets_returnsEmptyList() {
        when(assetRepository.findByUser_Id(1L)).thenReturn(List.of());

        List<Asset> result = assetService.getAssetsByUserId(1L);

        assertThat(result).isEmpty();
    }

    // --- updateAsset ---

    @Test
    void updateAsset_addsQuantityToExistingQuantity() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(asset)).thenReturn(asset);

        Asset result = assetService.updateAsset(1L, new BigDecimal("1.5"));

        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("3.5"));
    }

    @Test
    void updateAsset_withNegativeValue_reducesQuantity() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(asset)).thenReturn(asset);

        Asset result = assetService.updateAsset(1L, new BigDecimal("-1.0"));

        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    void updateAsset_whenAssetNotFound_throwsResourceNotFoundException() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.updateAsset(99L, BigDecimal.ONE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- findAssetByUserIdAndCoinId ---

    @Test
    void findAssetByUserIdAndCoinId_whenFound_returnsAsset() {
        when(assetRepository.findByUser_IdAndCoin_CoinId(1L, "bitcoin")).thenReturn(Optional.of(asset));

        Asset result = assetService.findAssetByUserIdAndCoinId(1L, "bitcoin");

        assertThat(result).isEqualTo(asset);
    }

    @Test
    void findAssetByUserIdAndCoinId_whenNotFound_returnsNull() {
        when(assetRepository.findByUser_IdAndCoin_CoinId(1L, "ethereum")).thenReturn(Optional.empty());

        Asset result = assetService.findAssetByUserIdAndCoinId(1L, "ethereum");

        assertThat(result).isNull();
    }

    // --- deleteAsset ---

    @Test
    void deleteAsset_whenFound_deletesAsset() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assetService.deleteAsset(1L);

        verify(assetRepository).delete(asset);
    }

    @Test
    void deleteAsset_whenNotFound_throwsResourceNotFoundException() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.deleteAsset(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
