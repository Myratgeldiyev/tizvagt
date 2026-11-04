# Tizvagt

A full-stack e-commerce application: a native **Android** client written in **Java** and a **Spring Boot** REST API backed by **MySQL**.

> 🚧 Work in progress. See the [issues](../../issues) for the roadmap.

## Tech Stack

| Layer    | Technologies |
|----------|--------------|
| Android  | Java 17, MVVM, Jetpack (ViewModel, LiveData, Navigation, Room), Hilt, Retrofit, Material 3 |
| Backend  | Java 17, Spring Boot 3, Spring Security + JWT, Spring Data JPA, Flyway |
| Database | MySQL 8 (Docker) |
| DevOps   | Docker Compose, GitHub Actions |

## Features

- [ ] User registration & login (JWT)
- [ ] Product catalog with categories, search and filtering
- [ ] Shopping cart & checkout (mock payment)
- [ ] Order history
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

Setup instructions will be added as each module is implemented.

## License

This project is licensed under the [MIT License](LICENSE).
