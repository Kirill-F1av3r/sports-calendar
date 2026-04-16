# Sports Calendar

`Sports Calendar` — backend-проект с микросервисной архитектурой для управления спортивными календарями и событиями.

## Что делает проект

Система позволяет:
- регистрировать и аутентифицировать пользователей;
- создавать персональные календари;
- добавлять события в календари;
- получать список календарей и детали календаря вместе с событиями.

Входной HTTP-трафик принимает API Gateway, который маршрутизирует запросы в соответствующие сервисы.

---

## Архитектура (микросервисный подход)

### Состав сервисов

1. **api-gateway** (`:8080`)
    - единая точка входа;
    - проксирует `/auth/**` в `auth-service`;
    - проксирует `/calendars/**` в `calendar-service`;
    - валидирует Bearer JWT для календарных маршрутов;
    - извлекает `userId` из токена и пробрасывает его во внутренний заголовок `X-User-Id`.

2. **auth-service** (`:8081`)
    - регистрация пользователей;
    - вход пользователя;
    - хранение пользователей в отдельной БД PostgreSQL;
    - хэширование паролей через BCrypt;
    - выпуск JWT access token.

3. **calendar-service** (`:8082`)
    - создание календарей;
    - получение календарей текущего пользователя;
    - создание и просмотр событий;
    - проверка владельца календаря (авторизация на уровне доменной логики);
    - хранение календарей и событий в отдельной БД PostgreSQL.

4. **shared (library module)**
    - общая библиотека с JWT-утилитами и DTO;
    - используется сервисами, где нужна единая JWT-логика.

### Инфраструктура данных

- `postgres-auth` (`localhost:5433`) — БД для `auth-service`.
- `postgres-calendar` (`localhost:5434`) — БД для `calendar-service`.

Каждый сервис имеет **собственную** БД (database-per-service), что соответствует базовому паттерну микросервисной архитектуры и уменьшает связанность между доменами.

### Поток запроса

#### 1) Регистрация / логин
`Client -> API Gateway -> Auth Service -> Auth DB`

- `POST /auth/register` создает пользователя.
- `POST /auth/login` возвращает access token.

#### 2) Работа с календарем
`Client (Bearer JWT) -> API Gateway -> Calendar Service -> Calendar DB`

- Gateway проверяет JWT.
- Gateway добавляет заголовок `X-User-Id`.
- Calendar Service использует `X-User-Id` как идентификатор владельца.

### Безопасность

- JWT подписывается по HS256 (`JWT_SECRET`, минимум 32 байта).
- У auth-service открыты `/auth/**`, остальные маршруты защищены Spring Security.
- В calendar-service доступ к календарю ограничен владельцем (`ownerId == X-User-Id`).
- API Gateway возвращает `401`, если нет корректного Bearer-токена для `/calendars/**`.

---

## API (через Gateway, порт `8080`)

### Auth

#### `POST /auth/register`
Создать пользователя.

Пример body:
```json
{
  "email": "user@example.com",
  "password": "strong-password",
  "fullName": "John Doe"
}
```

Успех: `201 Created`
```json
{
  "id": "uuid"
}
```

#### `POST /auth/login`
Получить JWT.

Пример body:
```json
{
  "email": "user@example.com",
  "password": "strong-password"
}
```

Успех: `200 OK`
```json
{
  "accessToken": "jwt",
  "refreshToken": "",
  "expiresIn": 900
}
```

### Calendars (требуется `Authorization: Bearer <token>`)

#### `POST /calendars`
Создать календарь.

```json
{
  "name": "Premier League",
  "sportType": "football"
}
```

Успех: `201 Created`
```json
{
  "id": "uuid"
}
```

#### `GET /calendars`
Список календарей текущего пользователя.

#### `GET /calendars/{id}`
Детали календаря + события.

#### `POST /calendars/{id}/events`
Добавить событие.

```json
{
  "title": "Arsenal vs Chelsea",
  "startDate": "2026-04-20",
  "endDate": "2026-04-20",
  "location": "London",
  "source": "manual"
}
```

---

## Технологии

- Java 21
- Spring Boot 3
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL 16
- Maven (multi-module)
- Docker / Docker Compose

---

## Структура репозитория

```text
.
├── docker-compose.yml
├── modules
│   ├── shared
│   ├── auth-service
│   ├── calendar-service
│   └── api-gateway
└── pom.xml
```

---

## Локальный запуск

### 1) Подготовка env

```bash
cp .env.example .env
```

Заполните секреты в `.env` (особенно `JWT_SECRET` и пароли БД).

### 2) Запуск в Docker

```bash
docker compose up --build
```

Сервисы будут доступны:
- Gateway: `http://localhost:8080`
- Auth Service: `http://localhost:8081`
- Calendar Service: `http://localhost:8082`
- Postgres Auth: `localhost:5433`
- Postgres Calendar: `localhost:5434`

---

## Запуск тестов

Из корня проекта:

```bash
mvn test
```

---
