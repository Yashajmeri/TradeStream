package com.example.TradeStream.orderService.service;

import com.example.TradeStream.assetService.entity.Asset;
import com.example.TradeStream.assetService.service.AssetService;
import com.example.TradeStream.orderService.entity.Order;
import com.example.TradeStream.orderService.entity.OrderItem;
import com.example.TradeStream.orderService.payload.OrderStatus;
import com.example.TradeStream.orderService.payload.OrderType;
import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.orderService.repository.OrderItemRepository;
import com.example.TradeStream.orderService.repository.OrderRepository;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.walletService.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final WalletService walletService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AssetService assetService;

    @Override
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        BigDecimal totalPrice = orderItem.getQuantity()
                .multiply(BigDecimal.valueOf(orderItem.getCoin().getCurrentPrice()))
                .setScale(8, RoundingMode.HALF_UP);
        Order order = new Order();
        order.setUser(user);
        order.setOrderType(orderType);
        order.setAmount(totalPrice);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTimestamp(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new APIException("Order not found with id: " + orderId));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Order> getOrdersByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUser_Id(userId, pageable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order processOrder(User user, BigDecimal quantity, Coin coin, OrderType orderType) {
        if (orderType.equals(OrderType.BUY)) {
            return buyAsset(user, quantity, coin);
        } else if (orderType.equals(OrderType.SELL)) {
            return sellAsset(user, quantity, coin);
        } else {
            throw new APIException("Invalid order type");
        }
    }


    private OrderItem createOrderItem(Coin coin, BigDecimal quantity, BigDecimal buyPrice, BigDecimal sellPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setCoin(coin);
        orderItem.setQuantity(quantity);
        orderItem.setBuyPrice(buyPrice);
        orderItem.setSellPrice(sellPrice);
        return orderItemRepository.save(orderItem);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order buyAsset(User user, BigDecimal quantity, Coin coin) throws APIException {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new APIException("Quantity must be greater than zero");
        }
        BigDecimal buyPrice = BigDecimal.valueOf(coin.getCurrentPrice());
        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, BigDecimal.ZERO);
        Order order = createOrder(user, orderItem, OrderType.BUY);
        orderItem.setOrder(order);
        orderItemRepository.save(orderItem);
        walletService.doOrderPayment(user, order);
        order.setOrderStatus(OrderStatus.SUCCESS);
        order.setOrderType(OrderType.BUY);
        order.setOrderItem(orderItem);
        Order savedOrder = orderRepository.save(order);
        Asset existingAsset = assetService.findAssetByUserIdAndCoinId(user.getId(), coin.getCoinId());
        if (existingAsset != null) {
            assetService.updateAsset(existingAsset.getId(), orderItem.getQuantity());
        } else {
            assetService.createAsset(user, orderItem.getCoin(), orderItem.getQuantity());
        }
        return savedOrder;
    }

@Transactional(rollbackFor = Exception.class)
public Order sellAsset(User user, BigDecimal quantity, Coin coin) {
    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
        throw new APIException("Quantity must be greater than zero");
    }

    Asset assetToSell = assetService.findAssetByUserIdAndCoinId(user.getId(), coin.getCoinId());

    if (assetToSell == null) {
        throw new APIException("Asset not found for user: " + user.getUsername() + ", coin: " + coin.getCoinId());
    }

    if (assetToSell.getQuantity().compareTo(quantity) < 0) {
        throw new APIException("Insufficient quantity of asset to sell");
    }

    BigDecimal buyPrice = assetToSell.getBuyPrice();
    BigDecimal sellPrice = BigDecimal.valueOf(coin.getCurrentPrice());

    OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, sellPrice);
    Order order = createOrder(user, orderItem, OrderType.SELL);

    order.setOrderStatus(OrderStatus.SUCCESS);
    order.setOrderType(OrderType.SELL);
    orderItem.setOrder(order);

    Order savedOrder = orderRepository.save(order);
    orderItemRepository.save(orderItem);

    walletService.doOrderPayment(user, savedOrder);

    Asset updatedAsset = assetService.updateAsset(assetToSell.getId(), quantity.negate());

    if (updatedAsset.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
        assetService.deleteAsset(assetToSell.getId());
    }

    return savedOrder;
}
}
