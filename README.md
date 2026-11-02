# CineBook: Movie Ticket Booking System
### Full-Stack Spring Boot + Pure JDBC + Multi-Threaded Java Socket Application (Strictly Aligned with the 6-Unit Java Syllabus)

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JDBC](https://img.shields.io/badge/Database-Pure%20JDBC%20%2B%20MySQL%208.0-blue.svg)](https://www.mysql.com/)
[![Networking](https://img.shields.io/badge/Java%20Sockets-TCP%20Port%208888-purple.svg)]()
[![Tests](https://img.shields.io/badge/Tests-33%20Passed%20(0%20Failures)-success.svg)]()

---

## 1. Strict 6-Unit Java Syllabus Mapping

Every Java class in `src/main/java` and `src/test/java` is restricted **exclusively** to concepts taught in the 6-unit Java course syllabus:

| Unit | Syllabus Topic | Exact Implementation in CineBook | Key Source Files |
| :--- | :--- | :--- | :--- |
| **Unit 1** | **Java Fundamentals** | Classes, Objects, Overloaded Constructors, `static` & `final` constant classes, **Array of Objects** (`PaymentProcessor[]`, `Thread[]`), Access Specifiers (`private`, `protected`, `public`), Inheritance (`extends BaseEntity`, `extends AbstractPaymentProcessor`, `super`), Method Overriding (`@Override`), **Dynamic Method Dispatch**, `abstract` classes/methods, and `interface`s (`PaymentProcessor`, `CrudRepository`, `SeatLockTask`). Pure-Java salted polynomial password hashing (`Random`, `char[]`, loops). | `com.movieticket.model.BaseEntity`, `com.movieticket.enums.*`, `com.movieticket.service.payment.*`, `com.movieticket.util.PasswordEncoder` |
| **Unit 2** | **Java Collections & Data Handling** | Generic Classes (`Pair<T, U>`, `BoundedBuffer<T>`, `ApiResponse<T>`), Generic Interface (`CrudRepository<T extends BaseEntity, ID>`), Generic Methods (`CollectionUtils.sortNaturally`, `sortWithComparator`, `takeFirst`), `ArrayList`, **`Comparable`** (`Movie`, `Seat`, `Show` with `Collections.sort()`), concrete **`Comparator`** classes (`MovieTitleComparator`, `MovieRatingComparator`, `BookingTimeDescComparator`), **`LinkedList`** (FIFO queue), and **`Map`s** (`HashMap`, `TreeMap`, `Map.Entry`), `HashSet`, `Iterator`. | `com.movieticket.util.Pair`, `com.movieticket.util.CollectionUtils`, `com.movieticket.model.comparator.*`, `com.movieticket.service.AuthService`, `com.movieticket.service.SeatService` |
| **Unit 3** | **Spring Boot** | Introduction to Web Programming & Spring Boot with Database: `@SpringBootApplication`, `SpringApplication.run`, `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@RequestBody`, Constructor Dependency Injection (`@Autowired`), `@Service`, `@Repository`, `@Component`, `@Value`, `ResponseEntity`, `CommandLineRunner`, `application.properties`. | `MovieTicketApplication.java`, `com.movieticket.controller.*`, `com.movieticket.service.*`, `com.movieticket.initializer.DataInitializer` |
| **Unit 4** | **Databases (SQL & Pure JDBC)** | Pure `java.sql.*` (`DriverManager.getConnection`, `Connection`, `Statement`, `PreparedStatement`, `ResultSet`, `SQLException`). SQL DDL (`CREATE TABLE IF NOT EXISTS` with `PRIMARY KEY`, `FOREIGN KEY`, `UNIQUE`), DML (`INSERT`, `UPDATE`, `DELETE`, batch inserts), DQL (`SELECT`, `WHERE`, `LIKE`, `DISTINCT`, 4-table `INNER JOIN`, `LEFT JOIN`, `GROUP BY`, `ORDER BY`, `COUNT`, `SUM`), and manual ACID JDBC Transactions (`setAutoCommit(false)`, `commit()`, `rollback()`). | `com.movieticket.db.DatabaseManager`, `com.movieticket.repository.*`, `com.movieticket.service.BookingService`, `com.movieticket.service.PaymentService` |
| **Unit 5** | **Networking using Java** | Multi-client TCP Server (`java.net.ServerSocket` on port `8888`), Client-Server `Socket` programming (`BufferedReader`, `InputStreamReader`, `PrintWriter`, `OutputStreamWriter`), `InetAddress` host/IP resolution, `URL` & `URLConnection` HTTP inspection, and interactive standalone `CinemaClient`. | `com.movieticket.network.CinemaSocketServer`, `com.movieticket.network.ClientHandler`, `com.movieticket.client.CinemaClient` |
| **Unit 6** | **Java for Operating System** | Multithreading (`extends Thread`, `implements Runnable`, `start()`, `run()`), **`Thread.join()`** barrier synchronization, **Race Condition prevention** via `synchronized` shared counter (`BookingMetricsCounter`), **Inter-Thread Communication** (`wait()` & `notifyAll()` Producer-Consumer `BoundedBuffer<T>` + `NotificationWorkerThread`), and **Deadlock Prevention** (Resource Ordering in `SeatLockManager` sorting seat IDs ascending to break Circular Wait). | `com.movieticket.os.BookingMetricsCounter`, `com.movieticket.os.BoundedBuffer`, `com.movieticket.os.NotificationWorkerThread`, `com.movieticket.os.TicketNotificationService`, `com.movieticket.os.SeatLockManager` |

---

## 2. How to Run

### Prerequisites
* **JDK 21+** installed (`java -version`)
* **MySQL 8.0+** running on `localhost:3306` (credentials configured in `src/main/resources/application.properties`)

### Step 1: Start the Spring Boot Web + JDBC + TCP Socket Server
```powershell
.\mvnw.cmd spring-boot:run
```
On startup:
1. `DatabaseManager` connects to MySQL via `DriverManager.getConnection(...)` and executes `CREATE TABLE IF NOT EXISTS` for all 10 normalized tables.
2. `DataInitializer` seeds default `ADMIN` and `CUSTOMER` accounts, 8 movies, 3 theatres, 6 screens (300 seats), and upcoming show schedules across all active screens, then runs a startup verification `Thread` synchronized with `join()`.
3. Spring Boot serves the Web UI & REST API on **http://localhost:8080**.
4. `CinemaSocketServer` starts a dedicated `Thread` listening on **TCP port `8888`**.

### Step 2: Access the Web Application
* **Home Page**: http://localhost:8080/
* **Movie Catalog**: http://localhost:8080/movies.html
* **Customer Login**: http://localhost:8080/login.html (`customer@cinebook.com` / `Customer@123`)
* **Admin Dashboard**: http://localhost:8080/admin.html (`admin@cinebook.com` / `Admin@123`)
* **Backend Unit 5 & 6 Live Diagnostics**: http://localhost:8080/status

### Step 3: Launch the Interactive Java TCP Socket Client (Unit 5)
In a second terminal:
```powershell
.\run_client.bat
# or
.\run_client.ps1
# or directly:
java -cp target/classes com.movieticket.client.CinemaClient
```

---

## 3. Architecture Overview

```
+-------------------------------------------------------------------------+
|   WEB FRONTEND (HTML/CSS/JS)          JAVA SOCKET CLIENT (CinemaClient) |
|   http://localhost:8080               java.net.Socket -> localhost:8888 |
+------------------+-----------------------------------+------------------+
                   | HTTP REST JSON                    | TCP Line Protocol
                   v                                   v
+-----------------------------------+  +----------------------------------+
| SPRING BOOT REST CONTROLLERS      |  | UNIT 5 NETWORKING LAYER          |
| @RestController (/api/*)          |  | ServerSocket(8888), ClientHandler|
| Movie, Show, Booking, Admin, Auth |  | InetAddress, URL, URLConnection  |
+------------------+----------------+  +---------------+------------------+
                   |                                   |
                   +-----------------+-----------------+
                                     | Constructor Dependency Injection
                                     v
+-------------------------------------------------------------------------+
|               SERVICE & UNIT 6 CONCURRENCY LAYER                        |
| - AuthService (synchronized HashMap session store)                      |
| - BookingService & PaymentService (PaymentProcessor[] polymorphism)     |
| - SeatLockManager (Ordered resource locking -> Deadlock prevention)     |
| - BookingMetricsCounter (synchronized shared counters -> No race cond.) |
| - TicketNotificationService + BoundedBuffer (wait / notifyAll)          |
+------------------------------------+------------------------------------+
                                     | Pure JDBC (java.sql.*)
                                     v
+-------------------------------------------------------------------------+
|                   PURE JDBC REPOSITORY & MYSQL LAYER                    |
| - DatabaseManager (DriverManager, manual commit() / rollback())         |
| - 10 JDBC Repositories (PreparedStatement, ResultSet, JOIN, GROUP BY)   |
| - MySQL Schema: movie_ticket_booking (10 normalized relational tables)  |
+-------------------------------------------------------------------------+
```

---

## 4. Relational Database Schema (`movie_ticket_booking`)

Managed via pure JDBC DDL in `DatabaseManager.java`:

| Table | Purpose | Key Constraints |
| :--- | :--- | :--- |
| `users` | Customer & Admin accounts | `PRIMARY KEY (id)`, `UNIQUE (email)` |
| `movies` | Movie catalog, duration, rating, status | `PRIMARY KEY (id)` |
| `theatres` | Multiplex venues & locations | `PRIMARY KEY (id)` |
| `screens` | Cinema halls within a theatre | `PRIMARY KEY (id)`, `FOREIGN KEY (theatre_id)` |
| `seats` | Tiered seat map (`REGULAR`, `PREMIUM`, `RECLINER`) | `PRIMARY KEY (id)`, `UNIQUE (screen_id, row_label, seat_number)` |
| `shows` | Scheduled movie screenings | `PRIMARY KEY (id)`, `FOREIGN KEY (movie_id, screen_id)` |
| `bookings` | Customer booking headers & totals | `PRIMARY KEY (id)`, `UNIQUE (booking_reference)` |
| `booking_seats` | Individual seats linked to a booking | `PRIMARY KEY (id)`, `FOREIGN KEY (booking_id, seat_id, show_id)` |
| `show_seat_reservations` | Active seat locks (`HELD` / `CONFIRMED`) | `PRIMARY KEY (id)`, `UNIQUE (show_id, seat_id)` |
| `payments` | Simulated Card / UPI / NetBanking records | `PRIMARY KEY (id)`, `UNIQUE (payment_reference)` |

---

## 5. Testing & Verification

### Run All 33 JUnit 5 Unit & Concurrency Tests
```powershell
.\mvnw.cmd clean test
```
Covers:
* `Unit6ConcurrencyTest`: Multi-threaded race-condition test (`10` threads × `200` increments with `Thread.join()`), Producer-Consumer `BoundedBuffer` (`wait()`/`notifyAll()`), and opposite-order `SeatLockManager` deadlock-prevention test.
* `AuthServiceTest`, `BookingServiceTest`, `CancellationServiceTest`, `MovieServiceTest`, `PaymentServiceTest`, `SeatServiceTest`, `ShowServiceTest`, `PasswordEncoderTest`.

### Run Automated End-to-End Live API & MySQL Verification
With the server running on `localhost:8080`:
```powershell
powershell -ExecutionPolicy Bypass -File .\test_e2e.ps1
```
