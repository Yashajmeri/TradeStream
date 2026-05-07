package com.example.TradeStream.orderService;

import com.example.TradeStream.assetService.entity.Asset;
import com.example.TradeStream.assetService.service.AssetService;
import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.orderService.entity.Order;
import com.example.TradeStream.orderService.entity.OrderItem;
import com.example.TradeStream.orderService.payload.OrderStatus;
import com.example.TradeStream.orderService.payload.OrderType;
import com.example.TradeStream.orderService.repository.OrderItemRepository;
import com.example.TradeStream.orderService.repository.OrderRepository;
import com.example.TradeStream.orderService.service.OrderServiceImpl;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.walletService.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private WalletService walletService;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private AssetService assetService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Coin coin;

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
    }

    // --- getOrderById ---

    @Test
    void getOrderById_whenFound_returnsOrder() {
        Order order = new Order();
        order.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getOrderById_whenNotFound_throwsAPIException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("99");
    }

    // --- getOrdersByUser ---

    @Test
    void getOrdersByUser_returnsPaginatedOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Order order = new Order();
        order.setId(1L);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findByUser_Id(1L, pageable)).thenReturn(page);

        Page<Order> result = orderService.getOrdersByUser(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // --- processOrder ---

    @Test
    void processOrder_invalidOrderType_throwsAPIException() {
        assertThatThrownBy(() -> orderService.processOrder(user, BigDecimal.ONE, coin, null))
                .isInstanceOf(Exception.class);
    }

    // --- buyAsset ---

    @Test
    void buyAsset_newAsset_createsOrderAndAsset() {
        BigDecimal quantity = new BigDecimal("0.5");

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
            OrderItem item = inv.getArgument(0);
            item.setId(1L);
            return item;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(null);

        Order result = orderService.buyAsset(user, quantity, coin);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.SUCCESS);
        assertThat(result.getOrderType()).isEqualTo(OrderType.BUY);
        verify(assetService).createAsset(eq(user), eq(coin), eq(quantity));
        verify(walletService).doOrderPayment(eq(user), any(Order.class));
    }

    @Test
    void buyAsset_existingAsset_updatesAssetQuantity() {
        BigDecimal quantity = new BigDecimal("0.2");
        Asset existingAsset = new Asset();
        existingAsset.setId(5L);
        existingAsset.setQuantity(new BigDecimal("1.0"));

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(existingAsset);

        Order result = orderService.buyAsset(user, quantity, coin);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.SUCCESS);
        verify(assetService).updateAsset(5L, quantity);
        verify(assetService, never()).createAsset(any(), any(), any());
    }

    @Test
    void buyAsset_zeroQuantity_throwsAPIException() {
        assertThatThrownBy(() -> orderService.buyAsset(user, BigDecimal.ZERO, coin))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void buyAsset_negativeQuantity_throwsAPIException() {
        assertThatThrownBy(() -> orderService.buyAsset(user, new BigDecimal("-1"), coin))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void buyAsset_orderAmountCalculatedCorrectly() {
        BigDecimal quantity = new BigDecimal("2.0");

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(assetService.findAssetByUserIdAndCoinId(any(), any())).thenReturn(null);

        Order result = orderService.buyAsset(user, quantity, coin);

        // 2.0 * 50000 = 100000
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100000.00000000"));
    }

    // --- sellAsset ---

    @Test
    void sellAsset_success_updatesWalletAndReducesAsset() {
        BigDecimal quantity = new BigDecimal("0.5");
        Asset asset = new Asset();
        asset.setId(5L);
        asset.setQuantity(new BigDecimal("1.0"));
        asset.setBuyPrice(new BigDecimal("45000.00"));
        asset.setCoin(coin);

        Asset updatedAsset = new Asset();
        updatedAsset.setId(5L);
        updatedAsset.setQuantity(new BigDecimal("0.5"));

        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(asset);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(2L);
            return o;
        });
        when(assetService.updateAsset(5L, quantity.negate())).thenReturn(updatedAsset);

        Order result = orderService.sellAsset(user, quantity, coin);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.SUCCESS);
        assertThat(result.getOrderType()).isEqualTo(OrderType.SELL);
        verify(walletService).doOrderPayment(eq(user), any(Order.class));
    }

    @Test
    void sellAsset_assetNotFound_throwsAPIException() {
        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(null);

        assertThatThrownBy(() -> orderService.sellAsset(user, new BigDecimal("0.5"), coin))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Asset not found");
    }

    @Test
    void sellAsset_insufficientQuantity_throwsAPIException() {
        Asset asset = new Asset();
        asset.setId(5L);
        asset.setQuantity(new BigDecimal("0.1"));
        asset.setBuyPrice(new BigDecimal("45000.00"));

        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(asset);

        assertThatThrownBy(() -> orderService.sellAsset(user, new BigDecimal("0.5"), coin))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Insufficient quantity");
    }

    @Test
    void sellAsset_zeroQuantity_throwsAPIException() {
        assertThatThrownBy(() -> orderService.sellAsset(user, BigDecimal.ZERO, coin))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void sellAsset_fullSell_deletesAsset() {
        BigDecimal quantity = new BigDecimal("1.0");
        Asset asset = new Asset();
        asset.setId(5L);
        asset.setQuantity(new BigDecimal("1.0"));
        asset.setBuyPrice(new BigDecimal("45000.00"));
        asset.setCoin(coin);

        Asset zeroedAsset = new Asset();
        zeroedAsset.setId(5L);
        zeroedAsset.setQuantity(BigDecimal.ZERO);

        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(asset);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(2L);
            return o;
        });
        when(assetService.updateAsset(5L, quantity.negate())).thenReturn(zeroedAsset);

        orderService.sellAsset(user, quantity, coin);

        verify(assetService).deleteAsset(5L);
    }

    // --- createOrder ---

    @Test
    void createOrder_setsCorrectFieldsAndPersists() {
        OrderItem orderItem = new OrderItem();
        orderItem.setCoin(coin);
        orderItem.setQuantity(new BigDecimal("1.0"));
        orderItem.setBuyPrice(BigDecimal.valueOf(coin.getCurrentPrice()));
        orderItem.setSellPrice(BigDecimal.ZERO);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        Order result = orderService.createOrder(user, orderItem, OrderType.BUY);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getOrderType()).isEqualTo(OrderType.BUY);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50000.00000000"));
        assertThat(result.getTimestamp()).isNotNull();
    }
}
