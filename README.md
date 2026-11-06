# Tizvagt

A full-stack e-commerce application: a native **Android** client written in **Java** and a **Spring Boot** REST API backed by **MySQL**.

> 🚧 Work in progress. See the [issues](../../issues) for the roadmap.

## Tech Stack

| Layer    | Technologies |
|----------|--------------|
| Android  | Java 17, MVVM, Jetpack (ViewModel, LiveData, Navigation, Room), Hilt, Retrofit, Material 3 |
| Backend  | Java 17, Spring Boot 4, Spring Security + JWT, Spring Data JPA, Flyway |
| Database | MySQL 8 (Docker) |
| DevOps   | Docker Compose, GitHub Actions |

## Features

- [x] User registration & login (JWT)
- [x] Product catalog with categories, search and filtering
- [x] Shopping cart & checkout (mock payment)
- [x] Order history & cancellation
- [ ] Favorites
- [ ] Admin API for managing products and images

## Repository Structure

```
tizvagt/
├── android/   # Android client app
├── backend/   # Spring Boot REST API
└── .github/   # CI workflows, issue & PR templates
```

## Getting Started

### Prerequisites

- JDK 17
- Docker

### Backend

```bash
# 1. (optional) override defaults
cp .env.example .env

# 2. start MySQL (exposed on localhost:3307)
docker compose up -d

# 3. run the API
cd backend
./gradlew bootRun
```

Check it is running: <http://localhost:8080/actuator/health>

Run tests (uses Testcontainers, Docker must be running):

```bash
cd backend
./gradlew test
```

## API

| Method | Endpoint             | Auth | Description              |
|--------|----------------------|------|--------------------------|
| POST   | `/api/auth/register` | –    | Create an account        |
| POST   | `/api/auth/login`    | –    | Log in, returns a JWT    |
| GET    | `/api/users/me`      | JWT  | Current user's profile   |
| GET    | `/api/products`      | –    | List products (search, filter, paging, sort) |
| GET    | `/api/products/{id}` | –    | Product details          |
| POST   | `/api/products/{id}/image` | ADMIN | Upload product image (JPEG/PNG/WEBP, max 5 MB) |
| GET    | `/api/categories`    | –    | List categories          |
| GET    | `/api/cart`          | JWT  | Current cart with totals |
| POST   | `/api/cart/items`    | JWT  | Add product to cart      |
| PATCH  | `/api/cart/items/{productId}` | JWT | Change quantity   |
| DELETE | `/api/cart/items/{productId}` | JWT | Remove product    |
| DELETE | `/api/cart`          | JWT  | Empty the cart           |
| POST   | `/api/orders`        | JWT  | Checkout (mock payment)  |
| GET    | `/api/orders`        | JWT  | Own order history        |
| GET    | `/api/orders/{id}`   | JWT  | Order details            |
| POST   | `/api/orders/{id}/cancel` | JWT | Cancel a paid order and restock |

Product list query parameters: `search`, `categoryId`, `minPrice`, `maxPrice`, `page`, `size` (max 100), `sort` (e.g. `price,asc`).

Authenticated requests send the token as `Authorization: Bearer <token>`.

## Design Notes

- **No overselling:** stock is decreased with a single atomic `UPDATE ... WHERE stock >= ?`, verified by a concurrent checkout test.
- **All-or-nothing checkout:** stock, order and cart changes run in one transaction.
- **Price snapshots:** order items store the name and price at checkout time.
- **Ownership checks:** orders are always queried by `id` *and* `userId`, so users can't read others' orders (IDOR).

## License

This project is licensed under the [MIT License](LICENSE).
