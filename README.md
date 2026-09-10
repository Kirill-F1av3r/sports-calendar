# Sports Calendar

Backend-проект для планирования спортивного календаря соревнований и экспорта календаря в Google Sheets.

Пользователь может:

- зарегистрироваться и войти в систему;
- создать календарь на конкретный год и вид спорта;
- добавить соревнования с датами, уровнем (например всероссийские, региональные и тд), дисциплинами, местом, ссылкой и приоритетом;
- просматривать события с фильтрацией, поиском, сортировкой и пагинацией;
- подключить Google account;
- асинхронно экспортировать календарь в Google Sheets.

## Стек

- Java 21
- Spring Boot 3
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Kafka
- Docker Compose
- Maven

## Архитектура

```text
Client / Frontend
  -> API Gateway
    -> Auth Service
    -> Calendar Service
    -> Export Service
    -> Integration Service

Kafka
  -> Export Worker Service
```

Снаружи публикуется только API Gateway:

```text
http://localhost:8080
```

Остальные сервисы доступны внутри docker network.

## Сервисы

### api-gateway

Единая HTTP-точка входа.

Отвечает за:

- проксирование запросов во внутренние сервисы;
- проверку JWT access token для приватных маршрутов;
- удаление входящего `X-User-Id` от клиента;
- установку доверенного `X-User-Id` на основе JWT;
- CORS для браузерного frontend.

Маршрутизация:

| Gateway path | Service | Auth |
| --- | --- | --- |
| `/auth/**` | `auth-service` | public |
| `/calendars/metadata` | `calendar-service` | public |
| `/calendars/**` | `calendar-service` | Bearer JWT |
| `/exports/**` | `export-service` | Bearer JWT |
| `/integrations/google/callback` | `integration-service` | public |
| `/integrations/**` | `integration-service` | Bearer JWT |

### auth-service

Отвечает за пользователей и токены:

- регистрация;
- login;
- BCrypt-хэширование паролей;
- выпуск JWT access token;
- выпуск opaque refresh token;
- хранение SHA-256 hash refresh token в БД;
- refresh token rotation;
- logout с отзывом refresh token.

Access token возвращается в JSON. Refresh token возвращается в `HttpOnly` cookie:

```http
Set-Cookie: refresh_token=...; Path=/auth; HttpOnly; SameSite=Lax
```

### calendar-service

Отвечает за календари и соревнования.

Основные части:

- `CalendarService` — CRUD календарей и проверка владельца;
- `EventService` — CRUD событий, фильтрация, пагинация, сортировка;
- `CalendarExportService` — подготовка данных для экспорта;
- `CalendarMetadataService` — справочники enum для frontend;
- `CalendarSpecifications` / `EventSpecifications` — динамические JPA-фильтры;
- `SortParser` — безопасный разбор query-параметра `sort`.

### integration-service

Отвечает за Google OAuth:

- создание Google OAuth URL;
- обработка callback;
- хранение подключённого Google account;
- шифрование Google refresh token;
- выдача short-lived Google access token внутренним сервисам;
- отключение интеграции.

### export-service

Отвечает за export job:

- создаёт задачу экспорта;
- проверяет доступ пользователя к календарю;
- хранит статус задачи;
- публикует событие в Kafka topic `export.jobs.requested`;
- принимает internal callbacks от worker;
- отдаёт клиенту статус экспорта.

Статусы:

```text
PENDING
PROCESSING
SUCCESS
FAILED
```

### export-worker-service

Отдельный worker:

- слушает Kafka topic `export.jobs.requested`;
- получает данные календаря из `calendar-service`;
- получает Google access token из `integration-service`;
- создаёт Google Spreadsheet;
- записывает соревнования в Google Sheets;
- сообщает результат в `export-service`.

## Модель календаря

Календарь:

- `id`
- `ownerId`
- `name`
- `sportType`
- `year`
- `createdAt`
- `updatedAt`

Событие:

- `id`
- `calendarId`
- `title`
- `startDate`
- `endDate`
- `competitionLevel`
- `competitionLevelTitle`
- `location`
- `externalUrl`
- `disciplines`
- `priority`
- `priorityTitle`
- `createdAt`
- `updatedAt`

Однодневное соревнование хранится как:

```text
startDate = 2027-05-18
endDate   = 2027-05-18
```

Многодневное соревнование:

```text
startDate = 2027-05-18
endDate   = 2027-05-20
```

Если при создании события `endDate` не передан, backend использует `startDate`.

## Enum-значения

### CompetitionLevel

| Code | Русское название |
| --- | --- |
| `INTERNATIONAL` | международные |
| `NATIONAL` | всероссийские |
| `REGIONAL` | региональные |
| `LOCAL` | местные |
| `TRAINING` | тренировочные |
| `OTHER` | другое |

Если `competitionLevel` не передан при создании события, используется `OTHER`.

### EventPriority

| Code | Русское название |
| --- | --- |
| `REQUIRED` | обязательный |
| `IMPORTANT` | важный |
| `OPTIONAL` | необязательный |

Если `priority` не передан при создании события, используется `OPTIONAL`.

## API

Все публичные запросы выполняются через gateway:

```text
http://localhost:8080
```

Для защищённых endpoint-ов нужен заголовок:

```http
Authorization: Bearer <accessToken>
```

### Auth

#### Register

```http
POST /auth/register
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "strong-password",
  "fullName": "Ivan Ivanov"
}
```

Ответ:

```json
{
  "id": "00000000-0000-0000-0000-000000000000"
}
```

#### Login

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "strong-password"
}
```

Ответ body:

```json
{
  "accessToken": "jwt-access-token",
  "expiresIn": 900
}
```

Ответ headers:

```http
Set-Cookie: refresh_token=...; Path=/auth; HttpOnly; SameSite=Lax
```

#### Refresh

```http
POST /auth/refresh
```

Refresh token берётся из cookie `refresh_token`.

Ответ:

```json
{
  "accessToken": "new-jwt-access-token",
  "expiresIn": 900
}
```

Также backend выставляет новый `refresh_token` cookie.

#### Logout

```http
POST /auth/logout
```

Logout отзывает refresh token и очищает cookie.

Важно: уже выданный access token остаётся валидным до истечения срока жизни.

### Calendar metadata

```http
GET /calendars/metadata
```

Endpoint публичный. Нужен frontend-у для отображения справочников.

Ответ:

```json
{
  "competitionLevels": [
    {
      "code": "REGIONAL",
      "title": "региональные"
    }
  ],
  "priorities": [
    {
      "code": "REQUIRED",
      "title": "обязательный"
    }
  ]
}
```

### Calendars

#### Create calendar

```http
POST /calendars
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "name": "Календарь ориентирования",
  "sportType": "спортивное ориентирование",
  "year": 2027
}
```

#### List calendars

```http
GET /calendars
Authorization: Bearer <accessToken>
```

Query-параметры:

| Parameter | Example | Description |
| --- | --- | --- |
| `year` | `2027` | фильтр по году |
| `sportType` | `спортивное ориентирование` | точный фильтр по виду спорта без учёта регистра |
| `search` | `ориент` | поиск по названию календаря и виду спорта |
| `sort` | `updated,desc` | сортировка |

Поддерживаемые sort-поля:

```text
updated
created
year
name
```

Примеры:

```http
GET /calendars?year=2027&sort=updated,desc
GET /calendars?search=ориент&sort=name,asc
```

#### Get calendar

```http
GET /calendars/{calendarId}
Authorization: Bearer <accessToken>
```

Возвращает только календарь, без событий.

#### Update calendar

```http
PATCH /calendars/{calendarId}
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "name": "Календарь ориентирования 2027",
  "sportType": "спортивное ориентирование",
  "year": 2027
}
```

Все поля опциональные.

#### Delete calendar

```http
DELETE /calendars/{calendarId}
Authorization: Bearer <accessToken>
```

Удаление календаря также удаляет его события.

### Events

#### Create event

```http
POST /calendars/{calendarId}/events
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "title": "Чемпионат области",
  "startDate": "2027-05-18",
  "endDate": "2027-05-20",
  "competitionLevel": "REGIONAL",
  "location": "Владимир",
  "externalUrl": "https://example.com/registration",
  "disciplines": [
    "кросс-классика",
    "кросс-лонг"
  ],
  "priority": "REQUIRED"
}
```

Для однодневного события можно не передавать `endDate`:

```json
{
  "title": "Контрольный старт",
  "startDate": "2027-06-01",
  "competitionLevel": "TRAINING",
  "priority": "OPTIONAL"
}
```

#### List events

```http
GET /calendars/{calendarId}/events
Authorization: Bearer <accessToken>
```

Query-параметры:

| Parameter | Example | Description |
| --- | --- | --- |
| `from` | `2027-05-01` | показать события, которые пересекаются с периодом начиная с этой даты |
| `to` | `2027-05-31` | показать события, которые пересекаются с периодом до этой даты |
| `competitionLevel` | `REGIONAL` | фильтр по уровню соревнований |
| `priority` | `REQUIRED` | фильтр по приоритету |
| `search` | `кросс` | поиск по названию, месту и дисциплинам |
| `page` | `0` | номер страницы, начиная с нуля |
| `size` | `20` | размер страницы, от 1 до 100 |
| `sort` | `date,asc` | сортировка |

Поддерживаемые sort-поля:

```text
date
title
priority
level
created
updated
```

Примеры:

```http
GET /calendars/{calendarId}/events?page=0&size=20&sort=date,asc
GET /calendars/{calendarId}/events?from=2027-05-01&to=2027-05-31
GET /calendars/{calendarId}/events?search=кросс&priority=REQUIRED
```

Ответ:

```json
{
  "content": [
    {
      "id": "00000000-0000-0000-0000-000000000000",
      "calendarId": "00000000-0000-0000-0000-000000000000",
      "title": "Чемпионат области",
      "startDate": "2027-05-18",
      "endDate": "2027-05-20",
      "competitionLevel": "REGIONAL",
      "competitionLevelTitle": "региональные",
      "location": "Владимир",
      "externalUrl": "https://example.com/registration",
      "disciplines": [
        "кросс-классика",
        "кросс-лонг"
      ],
      "priority": "REQUIRED",
      "priorityTitle": "обязательный",
      "createdAt": "2027-01-01T10:00:00Z",
      "updatedAt": "2027-01-01T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### Get event

```http
GET /calendars/{calendarId}/events/{eventId}
Authorization: Bearer <accessToken>
```

#### Update event

```http
PATCH /calendars/{calendarId}/events/{eventId}
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "title": "Обновлённое название",
  "startDate": "2027-05-18",
  "endDate": "2027-05-19",
  "competitionLevel": "NATIONAL",
  "location": "Москва",
  "externalUrl": "https://example.com",
  "disciplines": [
    "кросс-лонг"
  ],
  "priority": "IMPORTANT"
}
```

Все поля опциональные.

#### Delete event

```http
DELETE /calendars/{calendarId}/events/{eventId}
Authorization: Bearer <accessToken>
```

### Google integration

#### Connect

```http
GET /integrations/google/connect
Authorization: Bearer <accessToken>
```

Ответ:

```json
{
  "redirectUrl": "https://accounts.google.com/o/oauth2/v2/auth?..."
}
```

Пользователь открывает `redirectUrl`, подтверждает доступ, после чего Google редиректит на:

```http
GET /integrations/google/callback?code=...&state=...
```

#### Status

```http
GET /integrations/google/status
Authorization: Bearer <accessToken>
```

Ответ:

```json
{
  "provider": "GOOGLE",
  "connected": true,
  "email": null,
  "scopes": "https://www.googleapis.com/auth/drive.file"
}
```

#### Disconnect

```http
DELETE /integrations/google
Authorization: Bearer <accessToken>
```

### Export

#### Create export job

```http
POST /exports
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "calendarId": "00000000-0000-0000-0000-000000000000",
  "provider": "GOOGLE_SHEETS"
}
```

Ответ:

```json
{
  "jobId": "00000000-0000-0000-0000-000000000000",
  "provider": "GOOGLE_SHEETS",
  "status": "PENDING",
  "spreadsheetId": null,
  "spreadsheetUrl": null,
  "errorMessage": null
}
```

#### Get export job

```http
GET /exports/{jobId}
Authorization: Bearer <accessToken>
```

Успешный результат:

```json
{
  "jobId": "00000000-0000-0000-0000-000000000000",
  "provider": "GOOGLE_SHEETS",
  "status": "SUCCESS",
  "spreadsheetId": "google-spreadsheet-id",
  "spreadsheetUrl": "https://docs.google.com/spreadsheets/d/...",
  "errorMessage": null
}
```

## Google Sheets export flow

```text
Client
  -> GET /integrations/google/connect
  <- redirectUrl

User
  -> Google consent screen
  -> /integrations/google/callback

Client
  -> POST /exports
  <- 202 Accepted + jobId

Export Service
  -> Calendar Service: access-check
  -> Kafka: export.jobs.requested

Export Worker Service
  -> Calendar Service: export-data
  -> Integration Service: Google access token
  -> Google Sheets API: create spreadsheet
  -> Google Sheets API: write rows
  -> Export Service: SUCCESS / FAILED

Client
  -> GET /exports/{jobId}
  <- status + spreadsheetUrl
```

## Локальный запуск через Docker Compose

Скопировать пример переменных окружения:

```bash
cp .env.example .env
```

Для запуска без реального Google export можно оставить Google-переменные тестовыми, но export в Google Sheets работать не будет.

Для рабочего Google export нужно заполнить:

```text
GOOGLE_OAUTH_CLIENT_ID
GOOGLE_OAUTH_CLIENT_SECRET
GOOGLE_OAUTH_REDIRECT_URI=http://localhost:8080/integrations/google/callback
INTEGRATION_TOKEN_ENCRYPTION_SECRET
```

`JWT_SECRET` в `.env` должен быть не короче 32 ASCII-символов. Иначе `auth-service` и `api-gateway` не стартуют из-за `WeakKeyException`.

Затем:

```bash
docker compose up --build
```

Порты:

| Service | Internal port | Published port |
| --- | --- | --- |
| frontend | `5173` | `5173` |
| api-gateway | `8080` | `8080` |
| auth-service | `8081` | - |
| calendar-service | `8082` | - |
| export-service | `8083` | - |
| export-worker-service | `8084` | - |
| integration-service | `8085` | - |
| postgres-auth | `5432` | `5433` |
| postgres-calendar | `5432` | `5434` |
| postgres-export | `5432` | - |
| postgres-integration | `5432` | - |
| kafka | `9092` | - |

## Frontend app

Frontend находится в директории:

```text
frontend/
```

Технологии:

- Vite
- React
- TypeScript
- обычный CSS

Запуск вместе со всем проектом:

```bash
docker compose up --build
```

Открыть:

```text
http://localhost:5173
```

Запуск frontend локально при уже поднятом backend:

```bash
cd frontend
npm install
npm run dev
```

Frontend по умолчанию ходит в gateway:

```text
http://localhost:8080
```

Адрес gateway можно изменить через:

```text
VITE_API_BASE_URL=http://localhost:8080
```

## Frontend/CORS

Gateway разрешает CORS для origin из переменной:

```text
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Это рассчитано на frontend, запущенный локально через Vite.

Так как refresh token хранится в cookie, frontend должен отправлять запросы с credentials:

```js
fetch("http://localhost:8080/auth/refresh", {
  method: "POST",
  credentials: "include"
});
```

Для axios:

```js
axios.post("http://localhost:8080/auth/refresh", null, {
  withCredentials: true
});
```

## Тесты

Запуск всех тестов:

```bash
mvn test
```

Запуск проверки как в CI:

```bash
mvn -B -U clean verify
```

Запуск только calendar-service и зависимых модулей:

```bash
mvn -B -pl modules/calendar-service -am test
```

## CI

GitHub Actions workflow находится в:

```text
.github/workflows/ci.yml
```

CI запускается:

- при `push` в ветки `develop*` и `develop/**`;
- при `pull_request` в `main`, `develop*`, `develop/**`.

Команда CI:

```bash
mvn -B -U clean verify
```

