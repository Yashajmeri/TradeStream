package com.example.TradeStream.orderService.controller;

import com.example.TradeStream.cryptoCoinService.entity.Coin;
import com.example.TradeStream.cryptoCoinService.service.CoinService;
import com.example.TradeStream.orderService.entity.Order;
import com.example.TradeStream.orderService.payload.CreateOrderRequest;
import com.example.TradeStream.orderService.payload.OrderType;
import com.example.TradeStream.orderService.service.OrderService;
import com.example.TradeStream.userService.entity.User;
import com.example.TradeStream.userService.service.UserService;
import com.example.TradeStream.walletService.exception.APIException;
import com.example.TradeStream.walletService.service.WalletTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Orders", description = "Place and manage BUY/SELL crypto orders")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;
    private final CoinService coinService;
    private final WalletTransactionService walletTransactionService;

    @Operation(summary = "Place a BUY or SELL order", description = "Executes the order and debits/credits the user wallet immediately")
    @PostMapping("/pay")
    public ResponseEntity<Order> doOrderPayment(Authentication authentication, @Valid @RequestBody CreateOrderRequest request) {
        User user = userService.getUserByUserName(authentication.getName());
        Coin coin = coinService.getCoinById(request.getCoinId());
        Order order = orderService.processOrder(user, BigDecimal.valueOf(request.getQuantity()), coin, request.getOrderType());
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get order by ID (owner only)")
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId, Authentication authentication) {
        User user = userService.getUserByUserName(authentication.getName());
        Order order = orderService.getOrderById(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new APIException("Forbidden : You do not have authorization for this order!" + orderId);
        }
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get paginated order history for the authenticated user")
    @GetMapping("/my-orders")
    public ResponseEntity<Page<Order>> getUserOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        User user = userService.getUserByUserName(authentication.getName());
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by("timestamp").ascending()
                : Sort.by("timestamp").descending();
        return ResponseEntity.ok(
                orderService.getOrdersByUser(user.getId(), PageRequest.of(page, size, sort)));
    }
}