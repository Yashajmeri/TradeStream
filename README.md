# TradeStream

A full-featured cryptocurrency and stock trading backend built with Spring Boot. TradeStream provides real-time market data, order execution, wallet management, and payment processing through a secure REST API.

---

## Features

- **JWT Authentication** — stateless sign-up/sign-in with BCrypt password hashing and role-based access control
- **Order Execution** — transactional BUY/SELL orders with optimistic locking and automatic asset tracking
- **Wallet System** — per-user wallets with full transaction history, wallet-to-wallet transfers, and order payments
- **Live Market Data** — cryptocurrency prices via CoinGecko API, stock quotes and candlestick data via Twelve Data API
- **Real-Time Streaming** — Binance WebSocket integration for live kline/candle data
- **Payment Gateways** — Stripe and Razorpay integration for wallet top-ups
- **Withdrawal Workflow** — user-initiated withdrawals with admin approve/reject flow and automatic refunds
- **Watchlist** — per-user watchlists to track favourite coins
- **Rate Limiting** — per-IP and per-endpoint request throttling using Bucket4j

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 3.4.3 |
| Security | Spring Security + JJWT 0.13.0 |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 (development) / PostgreSQL (production) |
| Payments | Stripe Java 31.3.0, Razorpay Java 1.4.8 |
| Rate Limiting | Bucket4j 8.10.1 |
| Real-Time | Spring WebSocket (Binance feed) |
| Build | Maven |
| Utilities | Lombok |

---

## Architecture

The application is organized as a modular monolith. Each domain is self-contained with its own entity, repository, service, controller, and exception handling.

```
com.example.TradeStream/
├── userService/        # Authentication, user profiles, JWT, security config
├── orderService/       # BUY/SELL order processing
├── walletService/      # Wallet balances and transaction ledger
├── assetService/       # User crypto holdings
├── cryptoCoinService/  # CoinGecko market data
├── marketService/      # Twelve Data stock/crypto quotes and candles
├── paymentService/     # Stripe & Razorpay payment orders
├── withdrawalService/  # Withdrawal requests and admin approval
├── watchListService/   # Per-user coin watchlists
├── streamingService/   # Binance WebSocket real-time feed
└── common/             # External API rate limiting (Bucket4j)
```

### Order Flow

```
POST /api/orders/pay
       │
       ▼
  OrderService.processOrder()
       │
       ├─ createOrderItem()  →  persist OrderItem
       ├─ createOrder()      →  persist Order (PENDING)
       ├─ WalletService.doOrderPayment()  →  debit/credit wallet + transaction record
       ├─ Order status → SUCCESS
       └─ AssetService  →  create or update user asset holding
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- (Optional) PostgreSQL 14+ for production mode

### Clone and Run

```bash
git clone https://github.com/your-username/TradeStream.git
cd TradeStream
```

Copy the environment template and fill in your API keys:

```bash
cp .env.example .env
```

Run with Maven (uses H2 in-memory database by default):

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Environment Variables

Create a `.env` file or set the following in your environment before running:

| Variable | Description |
|---|---|
| `TWELVEDATA_API_KEY` | Twelve Data API key for stock/crypto market data |
| `COINGECKO_BASE_URL` | CoinGecko base URL (default: `https://api.coingecko.com/api/v3`) |
| `JWT_SECRET` | Secret key used to sign JWT tokens (min 32 chars) |
| `JWT_EXPIRATION_MS` | Token expiry in milliseconds (default: `86400000` — 24 hours) |
| `STRIPE_PUBLIC_KEY` | Stripe publishable key |
| `STRIPE_SECRET_KEY` | Stripe secret key |
| `RAZORPAY_API_KEY` | Razorpay key ID |
| `RAZORPAY_API_SECRET` | Razorpay key secret |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL (production only) |
| `SPRING_DATASOURCE_USERNAME` | Database username (production only) |
| `SPRING_DATASOURCE_PASSWORD` | Database password (production only) |

### Production Database (PostgreSQL)

Uncomment the PostgreSQL section in `src/main/resources/application.properties` and comment out the H2 block:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

---

## API Reference

All endpoints except `/auth/**` require a `Bearer <token>` header obtained from sign-in.

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/auth/signup` | Register a new user |
| `POST` | `/auth/signin` | Sign in and receive JWT token |
| `POST` | `/auth/signout` | Invalidate current session |

**Sign-up request body:**
```json
{
  "username": "alice",
  "fullName": "Alice Smith",
  "email": "alice@example.com",
  "password": "secret123",
  "role": "ROLE_USER"
}
```

**Sign-in response:**
```json
{
  "token": "<jwt>",
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "role": "ROLE_USER"
}
```

---

### Orders

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/orders/pay` | Place a BUY or SELL order |
| `GET` | `/api/orders/{orderId}` | Get order by ID (owner only) |
| `GET` | `/api/orders/my-orders` | Paginated order history for current user |

**Place order request body:**
```json
{
  "coinId": "bitcoin",
  "orderType": "BUY",
  "quantity": 0.5
}
```

---

### Wallet

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/wallet` | Get current user's wallet |
| `PUT` | `/api/wallet/{walletId}/transfer` | Transfer funds to another wallet |
| `PUT` | `/api/wallet/order/{orderId}/pay` | Pay for an order from wallet |
| `PUT` | `/api/wallet/deposit` | Add funds after payment verification |
| `GET` | `/api/transactions` | Paginated wallet transaction history |

---

### Market Data

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/market/coins` | List all coins with market data |
| `GET` | `/api/coins/{coinId}` | Get details for a specific coin |
| `GET` | `/api/coins/search?query=` | Search coins by name or symbol |
| `GET` | `/api/coins/top50` | Top 50 coins by market cap |
| `GET` | `/api/coins/trading` | Trending coins |
| `GET` | `/api/market/stock/quotes/{symbol}` | Stock quote |
| `GET` | `/api/market/stock/candles/{symbol}` | Candlestick (OHLCV) data |
| `GET` | `/api/market/status` | Current market open/close status |

---

### Payments

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/payment/{paymentMethod}/amount/{amount}` | Create a payment order (`STRIPE` or `RAZORPAY`) |
| `POST` | `/api/payment-details` | Save bank account details |
| `GET` | `/api/payment-details` | List saved payment details |

---

### Withdrawals

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/withdrawals/{amount}` | Request a withdrawal |
| `GET` | `/api/withdrawals/history` | Get withdrawal history for current user |
| `POST` | `/api/admin/withdrawals/{id}/process/{accept}` | Approve or reject a withdrawal (Admin) |
| `GET` | `/api/admin/withdrawals` | List all withdrawals with pagination (Admin) |

---

### Assets

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/assets` | List all assets held by current user |
| `GET` | `/api/assets/coin/{coinId}/user` | Get holding for a specific coin |
| `GET` | `/api/admin/assets/{assetId}` | Get asset by ID (Admin) |

---

### Watchlist

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/watchlist/user` | Get or create watchlist for current user |
| `PATCH` | `/api/watchlist/add/coin/{coinId}` | Add a coin to watchlist |
| `PATCH` | `/api/watchlist/remove/coin/{coinId}` | Remove a coin from watchlist |

---

### Real-Time Streaming

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/stream/coin/{symbol}` | Subscribe to Binance live price stream for a symbol |

---

## Rate Limits

| Scope | Limit |
|---|---|
| `POST /auth/signin` | 5 requests / minute |
| `POST /auth/signup` | 3 requests / 10 minutes |
| `POST /api/payment/**` | 5 requests / minute |
| `POST /api/orders/**` | 10 requests / minute |
| `GET /api/**` (general) | 100 requests / minute |
| CoinGecko API calls | 30 requests / minute |
| Twelve Data API calls | 8 requests / minute |

---

## Security

- Passwords hashed with BCrypt
- Stateless JWT authentication (no server-side session)
- CSRF disabled (JWT-based API)
- CORS configured for `localhost:3000` and `localhost:4200`
- Role-based access control: `ROLE_USER` and `ROLE_ADMIN`
- Admin endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`

---

## Testing

```bash
mvn test
```

The test suite contains 71 unit tests covering all five core services:

| Test Class | Tests | Service |
|---|---|---|
| `WalletServiceImplTest` | 16 | Wallet operations, transfers, order payments |
| `OrderServiceImplTest` | 15 | Buy/sell order flow, asset management |
| `AssetServiceImplTest` | 14 | Asset CRUD, quantity updates |
| `WithdrawalServiceImplTest` | 14 | Withdrawal request, approval, refund |
| `WatchListServiceImplTest` | 12 | Watchlist create, add/remove coins |
| `TradeStreamApplicationTests` | 2 | Spring context load, service wiring |

---

## Project Structure

```
TradeStream/
├── src/
│   ├── main/
│   │   ├── java/com/example/TradeStream/
│   │   │   ├── userService/
│   │   │   ├── orderService/
│   │   │   ├── walletService/
│   │   │   ├── assetService/
│   │   │   ├── cryptoCoinService/
│   │   │   ├── marketService/
│   │   │   ├── paymentService/
│   │   │   ├── withdrawalService/
│   │   │   ├── watchListService/
│   │   │   ├── streamingService/
│   │   │   └── common/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/example/TradeStream/
│           ├── walletService/
│           ├── orderService/
│           ├── assetService/
│           ├── withdrawalService/
│           └── watchListService/
└── pom.xml
```

---

## License

This project is for educational and portfolio purposes.
