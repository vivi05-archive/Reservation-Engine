# Reservation-Engine
# Inventory Reservation Engine

A Java and Spring Boot project that prevents **overselling** when many users reserve the same stock at the same time. It compares three concurrency strategies and includes a live simulator to prove which ones are correct.

## Problem

Stock is checked first and reduced later. If two threads run the check before either has updated the stock, both succeed and the shop sells units that do not exist (a **race condition**).

## Features

- Add items, view live stock, restock
- Reserve an item and hold it for **60 seconds**
- Confirm, cancel, or let it expire automatically (stock is returned)
- Three concurrency modes: **UNSAFE**, **MUTEX**, **CAS**
- Simulator that fires up to 200 users at the same instant and reports the result
- Live activity log showing which thread did what
- Web dashboard (single HTML page)

## Concurrency modes

| Mode | How it works | Result |
|---|---|---|
| UNSAFE | Check then update, no protection | Overselling and lost updates are possible |
| MUTEX | One lock per item around check and update | Correct; other threads wait |
| CAS | Compare-and-swap on a version column, retry on failure | Correct; no locks, losers retry |

## Reservation lifecycle

`ACTIVE` becomes `CONFIRMED`, `CANCELLED` or `EXPIRED`. Each change is one conditional update, so stock is returned exactly once.

## Tech stack

- Java (JDK 21 or higher)
- Spring Boot, Spring Data JPA, Spring MVC
- Gradle
- H2 database (in-memory)
- HTML, CSS, JavaScript

## Project structure

```
src/main/java/com/example/inventory
├── config        AppConfig, DataSeeder
├── controller    InventoryController (REST API)
├── dto           Dtos (request and response shapes)
├── entity        Item, Reservation, ReservationStatus
├── exception     InventoryException, GlobalExceptionHandler
├── repository    ItemRepository, ReservationRepository, StockSnapshot
└── service       ReservationService, InventoryService, SimulationService,
                  ItemLocks, ActivityLog, ExpiryJob, ReserveMode, ReserveResult
src/main/resources
├── application.properties
└── static/index.html
```

## How to run

1. Install JDK 21 or higher.
2. Open the project folder in a terminal.
3. Run:
   - Windows PowerShell: `.\gradlew bootRun`
   - Mac or Linux: `./gradlew bootRun`
4. Wait for `Started InventoryApplication`.
5. Open http://localhost:8080
6. Stop the server with `Ctrl + C`.

The H2 console is at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:inventorydb`, user `sa`, empty password).

## How to demo

1. Reserve, confirm and cancel an item from the dashboard.
2. In **Simulate concurrent users**, choose Laptop, 50 users, qty 1, starting stock 10.
3. Run **UNSAFE** and note the RACE result.
4. Run **MUTEX** and **CAS** and note the CORRECT results.
5. Open the activity log to see the threads.

## REST API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/items` | List items |
| POST | `/api/items` | Add an item |
| POST | `/api/items/{id}/restock` | Add stock |
| GET | `/api/reservations` | List reservations |
| POST | `/api/reservations` | Reserve (choose mode) |
| POST | `/api/reservations/{id}/confirm` | Confirm |
| POST | `/api/reservations/{id}/cancel` | Cancel and return stock |
| POST | `/api/simulate` | Run the simulation |
| GET / DELETE | `/api/log` | Read or clear the activity log |

## Configuration

`src/main/resources/application.properties`

- `inventory.processing-delay-ms=5` adds a small delay between check and update so the race is easy to see (set to 0 to disable)
- The hold time is `HOLD_SECONDS` in `ReservationService`

## Limitations

- Data is in memory and resets on restart
- The mutex works inside a single server only
- No user accounts, and one item per reservation

## Future scope

- Persistent database (MySQL or PostgreSQL)
- Distributed locking across several servers
- User accounts, a cart and payment

## Authors

- Vidushi Mishra (RA2511056030042)
- Prarthana Mittal (RA2511056030035)
