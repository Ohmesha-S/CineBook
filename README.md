# CineBook: Movie Ticket Booking System
### Pure Java Console & Network Socket Application (Aligned Strictly with 6 Core Syllabus Concepts)

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-blue.svg)](https://www.mysql.com/)
[![Networking](https://img.shields.io/badge/Java%20Sockets-TCP%20Port%208888-purple.svg)]()
[![Tests](https://img.shields.io/badge/Tests-33%20Passed%20(0%20Failures)-success.svg)]()

---

## 1. Syllabus Concepts Mapping

This project is strictly designed and implemented around the **6 Core Academic Java Concepts**:

| # | Syllabus Concept | Implementation in this Application | Key Source Files |
|---|------------------|------------------------------------|------------------|
| **1** | **Java Fundamentals** | Object-Oriented Programming (Polymorphism, Abstraction, Inheritance, Encapsulation), Custom Exceptions, Enums, Interfaces, Cryptography (`MessageDigest` SHA-256 with salt in `PasswordEncoder`). | `com.movieticket.model.*`, `com.movieticket.util.PasswordEncoder`, `com.movieticket.exception.*`, `com.movieticket.service.payment.*` |
| **2** | **Java Collections & Data Handling** | Deep use of `List`, `Set`, `Map`, `Queue`, Java Streams API for filtering, grouping, and pricing, formatted File I/O (`java.nio.file.Files`) for ticket generation. | `com.movieticket.service.*`, `com.movieticket.dto.*`, `com.movieticket.os.OsTicketPrinter` |
| **3** | **Spring Boot** | `@SpringBootApplication`, Dependency Injection (`@Service`, `@Repository`), Spring Boot lifecycle (`CommandLineRunner`), Spring Data JPA, `@Transactional` boundaries. | `MovieTicketApplication.java`, `com.movieticket.service.*`, `com.movieticket.initializer.*` |
| **4** | **Databases** | MySQL relational database persistence via JPA / Hibernate, relational constraints, ACID transactions, double-booking prevention, query repositories. | `com.movieticket.model.*`, `com.movieticket.repository.*` |
| **5** | **Networking using Java** | Multi-threaded TCP `ServerSocket` on port `8888`, worker `ClientHandler` (`Runnable`), core `Socket` I/O streams (`BufferedReader`, `PrintWriter`), standalone interactive `CinemaClient`. | `com.movieticket.network.CinemaSocketServer`, `com.movieticket.network.ClientHandler`, `com.movieticket.client.CinemaClient` |
| **6** | **Java for Operating System** | `ProcessBuilder` launching native OS viewer (`notepad.exe` / `xdg-open`) for booked tickets, `ProcessHandle` for OS PID inspection, `Runtime` memory & CPU inspection, thread pooling, OS shutdown hooks. | `com.movieticket.os.OsTicketPrinter`, `com.movieticket.os.OsSystemInspector` |

---

## 2. Project Description
**CineBook** is an enterprise-grade pure Java cinema ticket reservation system. It operates via a multi-threaded Java TCP Socket Server powered by Spring Boot and MySQL, paired with an interactive Java Console Client.

All operations—movie catalog querying, seat layout inspection, tiered pricing calculations, concurrency-safe seat booking, payment simulation, digital ticket generation, cancellation seat release, and OS receipt viewing—are executed purely in Java.

---

## 3. How to Run

### Step 1: Start the Spring Boot & Socket Server
```bash
.\mvnw.cmd spring-boot:run
```
* The server initializes the MySQL database schema and seeds initial movies, theatres, screens, and shows.
* The Java TCP `ServerSocket` begins listening on port **8888**.

### Step 2: Launch the Java Console Client
In another terminal:
```bash
.\run_client.bat
# Or:
.\run_client.ps1
# Or directly via Java:
java -cp target/classes com.movieticket.client.CinemaClient
```

---

## 4. Technology Stack
* **Language**: Java 21 (LTS).
* **Framework**: Spring Boot 3.3.5 (Core, Data JPA, Validation).
* **Database**: MySQL 8.0+ via Hibernate ORM & HikariCP.
* **Networking**: Core Java Sockets (`java.net.ServerSocket`, `java.net.Socket`).
* **Operating System**: Java `ProcessBuilder`, `Runtime`, `ProcessHandle`, `java.nio.file`.
* **Testing**: JUnit 5, Mockito.

---

## 5. Architecture Explanation

```
+-------------------------------------------------------------------------+
|                  JAVA CONSOLE CLIENT (CinemaClient.java)                |
|  - Interactive terminal menus (ANSI/Text)                               |
|  - Connected via java.net.Socket to localhost:8888                      |
+------------------------------------+------------------------------------+
                                     | Pure TCP Sockets (Request / Response Protocol)
                                     v
+-------------------------------------------------------------------------+
|               NETWORK SERVER LAYER (CinemaSocketServer.java)            |
|  - java.net.ServerSocket(8888)                                          |
|  - Multi-threaded ClientHandler (Runnable worker threads)               |
+------------------------------------+------------------------------------+
                                     | Dependency Injection
                                     v
+-------------------------------------------------------------------------+
|                  SPRING BOOT SERVICE LAYER (Java Logic)                 |
|  - MovieService, ShowService, SeatService, BookingService, AuthService  |
|  - Payment Processors (Polymorphism / Strategy Pattern)                 |
|  - OsTicketPrinter (ProcessBuilder) & OsSystemInspector (Runtime)       |
+------------------------------------+------------------------------------+
                                     | Spring Data JPA / Hibernate
                                     v
+-------------------------------------------------------------------------+
|                         DATABASE LAYER (MySQL)                          |
|  - Schema: movie_ticket_booking (ACID Transactions, Foreign Keys)       |
+-------------------------------------------------------------------------+
```
### Layer Interactions
1. **Controller Layer**: Handles incoming HTTP requests, validates incoming DTOs using Jakarta Validation, extracts authenticated user context, and invokes appropriate service methods.
2. **Service Layer**: Implements core business rules (e.g. past show validation, seat price computation, show scheduling conflict prevention, transactional booking state transitions).
3. **Repository Layer**: Extends Spring Data JPA interfaces (`JpaRepository`), providing type-safe CRUD operations, JPQL queries, and database projection.
4. **Database Layer**: Enforces referential integrity, foreign key cascades, and unique constraints preventing race conditions.

---

## 10. Database Design

The relational database is named **`movie_ticket_booking`** and contains 10 normalized tables:

| Table Name | Description | Key Constraints |
| :--- | :--- | :--- |
| **`users`** | Customer & administrator accounts | PK: `id`, Unique: `email` |
| **`movies`** | Movie catalog, ratings, durations, posters | PK: `id` |
| **`theatres`** | Cinema complexes and multiplex venues | PK: `id` |
| **`screens`** | Individual halls/screens within a theatre | PK: `id`, FK: `theatre_id` |
| **`seats`** | Physical seat layout for a screen (A1, A2...) | PK: `id`, FK: `screen_id`, Unique: (`screen_id`, `row_label`, `seat_number`) |
| **`shows`** | Scheduled screenings linking movies & screens | PK: `id`, FK: `movie_id`, FK: `screen_id` |
| **`bookings`** | Customer booking orders and financial total | PK: `id`, Unique: `booking_reference`, FK: `user_id`, FK: `show_id` |
| **`booking_seats`** | Seats assigned to a confirmed booking | PK: `id`, FK: `booking_id`, FK: `seat_id`, FK: `show_id` |
| **`show_seat_reservations`** | Active transactional seat locks & holds | PK: `id`, FK: `show_id`, FK: `seat_id`, FK: `user_id` |
| **`payments`** | Simulated transaction logs & audit trail | PK: `id`, FK: `booking_id`, Unique: `payment_reference` |

---

## 11. Database Relationships
1. **`User` 1 : N `Booking`**: One customer can place multiple bookings over time.
2. **`Theatre` 1 : N `Screen`**: One cinema complex contains multiple screens (IMAX, Dolby Atmos, Gold Class).
3. **`Screen` 1 : N `Seat`**: A screen defines the physical layout of seats with rows and categories.
4. **`Movie` 1 : N `Show`**: A single movie can have multiple showtimes scheduled across different theatres.
5. **`Screen` 1 : N `Show`**: A screen hosts multiple distinct showtimes, validated so timings never overlap.
6. **`Show` 1 : N `Booking`**: Multiple bookings belong to a single screening.
7. **`Booking` 1 : N `BookingSeat`**: A booking can encompass multiple seats (e.g. family or group bookings).
8. **`Booking` 1 : N `Payment`**: Records simulated payment attempts associated with a booking reference.

---

## 12. Project Folder Structure

```
Movie ticket booking/
├── .mvn/wrapper/                  # Maven wrapper configuration & JAR
├── pom.xml                        # Maven dependency & build configuration
├── README.md                      # Complete system documentation
├── test_e2e.ps1                   # End-to-end automated PowerShell verification suite
├── src/
│   ├── main/
│   │   ├── java/com/movieticket/
│   │   │   ├── MovieTicketApplication.java   # Spring Boot bootstrap class
│   │   │   ├── MovieTicketApplication.java   # Spring Boot bootstrap class
│   │   │   ├── client/                       # Concept 5: Interactive Console Client
│   │   │   │   └── CinemaClient.java
│   │   │   ├── network/                      # Concept 5: Java TCP Sockets
│   │   │   │   ├── CinemaSocketServer.java
│   │   │   │   └── ClientHandler.java
│   │   │   ├── os/                           # Concept 6: Java for Operating System
│   │   │   │   ├── OsTicketPrinter.java      # ProcessBuilder & native viewer
│   │   │   │   └── OsSystemInspector.java    # Runtime & OS diagnostics
│   │   │   ├── util/                         # Concept 1: Java Fundamentals
│   │   │   │   └── PasswordEncoder.java      # MessageDigest SHA-256 with salt
│   │   │   ├── controller/                   # REST API controllers
│   │   │   ├── dto/                          # Data Transfer Objects
│   │   │   ├── enums/                        # Strongly-typed domain enumerations
│   │   │   ├── exception/                    # Custom Exception hierarchy
│   │   │   ├── initializer/                  # Database seeder (CommandLineRunner)
│   │   │   ├── model/                        # JPA Entity classes (MySQL schema)
│   │   │   ├── repository/                   # Spring Data JPA repositories
│   │   │   └── service/                      # Core business services
│   │   │       ├── AuthService.java
│   │   │       ├── BookingService.java
│   │   │       ├── DashboardService.java
│   │   │       ├── MovieService.java
│   │   │       ├── PaymentService.java
│   │   │       ├── ScreenService.java
│   │   │       ├── SeatService.java
│   │   │       ├── ShowService.java
│   │   │       ├── TheatreService.java
│   │   │       ├── TicketService.java
│   │   │       └── payment/                  # Strategy Pattern payment processors
│   │   └── resources/
│   │       └── application.properties        # DB, socket port, and OS properties
├── run_client.bat                             # Windows launcher for Java console client
├── run_client.ps1                             # PowerShell launcher for client
│   │           └── js/                       # Client controllers
│   │               ├── admin.js
│   │               ├── api.js
│   │               ├── auth.js
│   │               ├── bookings.js
│   │               ├── home.js
│   │               ├── movie-details.js
│   │               ├── movies.js
│   │               ├── payment.js
│   │               ├── seat-selection.js
│   │               └── ticket.js
│   └── test/
│       ├── java/com/movieticket/service/     # Unit tests with Mockito & JUnit 5
│       │   ├── AuthServiceTest.java
│       │   ├── BookingServiceTest.java
│       │   ├── CancellationServiceTest.java
│       │   ├── MovieServiceTest.java
│       │   ├── PaymentServiceTest.java
│       │   ├── SeatServiceTest.java
│       │   └── ShowServiceTest.java
│       └── resources/mockito-extensions/
│           └── org.mockito.plugins.MockMaker # Java 21+ mock-maker configuration
```

---

## 13. Installation Instructions

### Prerequisites
* **Java Development Kit (JDK)**: Java 21 or higher installed (`java -version`).
* **MySQL Server**: MySQL 8.0+ running on `localhost:3306`.
* **Git**: (Optional) for version control.

### Setup Steps
1. Open PowerShell or Command Prompt.
2. Navigate to the project root directory:
   ```powershell
   cd "c:\Users\OHMESHA\Movie ticket booking"
   ```

---

## 14. MySQL Configuration

1. Log into your MySQL console:
   ```bash
   mysql -u root -p
   ```
2. Verify or create the database:
   ```sql
   CREATE DATABASE IF NOT EXISTS movie_ticket_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Update connection credentials in `src/main/resources/application.properties` if your MySQL username or password differs:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/movie_ticket_booking?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```

---

## 15. Backend Execution Instructions

Run the application using the included Maven wrapper:

```powershell
# Clean build and compile
.\mvnw.cmd clean compile

# Run the Spring Boot application
.\mvnw.cmd spring-boot:run
```

Once started, the console will confirm:
```
Tomcat started on port 8080 (http) with context path '/'
Started MovieTicketApplication in X.XX seconds
```

---

## 16. Frontend Access Instructions

Open any modern web browser and navigate to:

* **Customer Portal**: [http://localhost:8080/](http://localhost:8080/)
* **Movie Catalog**: [http://localhost:8080/movies.html](http://localhost:8080/movies.html)
* **Customer Login**: [http://localhost:8080/login.html](http://localhost:8080/login.html)
* **Customer Registration**: [http://localhost:8080/register.html](http://localhost:8080/register.html)
* **My Bookings**: [http://localhost:8080/my-bookings.html](http://localhost:8080/my-bookings.html)
* **Admin Dashboard**: [http://localhost:8080/admin.html](http://localhost:8080/admin.html)

---

## 17. Admin Initialization Instructions

On first startup, CineBook automatically seeds a default administrator account into MySQL:

* **Admin Email**: `admin@cinebook.com`
* **Admin Password**: `Admin@123`
* **Assigned Role**: `ADMIN`

### Admin Capabilities
* **Full Metrics Dashboard**: Live counters for total movies, theatres, screens, shows, confirmed bookings, and total system revenue.
* **Movie Management**: Create new movies, edit details, and set status (`NOW_SHOWING`, `UPCOMING`, `INACTIVE`).
* **Theatre & Screen Management**: Add cinema complexes and configure screens with automatic tiered seat generation.
* **Show Scheduling**: Schedule movie screenings with automated overlap validation preventing conflicting screen bookings.
* **Booking Audit**: Real-time view of customer reservations and transaction amounts.

---

## 18. Customer Registration and Login Instructions

1. Navigate to [Register](http://localhost:8080/register.html).
2. Enter Full Name, Email, Phone Number, and Password.
3. Upon registration, you are redirected to [Login](http://localhost:8080/login.html).
4. Alternatively, use the pre-seeded customer account:
   * **Email**: `customer@cinebook.com`
   * **Password**: `Customer@123`
   * **Role**: `CUSTOMER`

---

## 19. Booking Workflow

```
[ Browse Movies ] 
       │
       ▼
[ View Movie Details ] ──► [ Choose Theatre & Show Date/Time ]
                                       │
                                       ▼
                              [ Interactive Seat Map ]
                              (Regular / Premium / Recliner)
                                       │
                                       ▼
                              [ Review Summary ]
                              (Backend calculates total + fee)
                                       │
                                       ▼
                              [ Simulated Payment ]
                              (Card / UPI / NetBanking)
                                       │
                  ┌────────────────────┴────────────────────┐
                  ▼                                         ▼
            [ SUCCESS ]                               [ FAILED ]
                 │                                         │
                 ▼                                         ▼
   • Booking Status: CONFIRMED                • Seats Released Immediately
   • Seat Locks: CONFIRMED                    • Retry Payment Option
   • Digital Ticket Issued
   • Available in "My Bookings"
```

1. **Movie Selection**: Pick a movie from the home page carousel or the searchable catalog.
2. **Showtime Selection**: Choose a theatre and screening date/time. Past shows are automatically disabled.
3. **Seat Selection**: Select available seats in the interactive layout. Real-time subtotal updates dynamically.
4. **Checkout Hold**: Placing a booking triggers a transactional `HELD` record in MySQL with a 10-minute expiry.
5. **Double-Booking Shield**: Any concurrent customer attempting to select the same seats is rejected with `409 Conflict`.
6. **Payment & Ticket**: Choosing payment method and completing checkout transitions the order to `CONFIRMED` and generates the printable digital ticket.

---

## 20. Simulated Payment Explanation

To ensure academic safety without handling real credit card or UPI credentials, CineBook implements a **Simulated Payment Gateway** utilizing the **Strategy Design Pattern**:
* **`PaymentProcessor` Interface**: Common contract defining `processPayment(PaymentRequest, Booking)`.
* **`CardPaymentProcessor`**: Simulates credit and debit card card network authorizations.
* **`UpiPaymentProcessor`**: Simulates Virtual Payment Address (VPA) handshakes and UPI instant settlements.
* **`NetBankingPaymentProcessor`**: Simulates core-banking portal redirects and confirmations.
* **Deterministic Simulation**: Users can simulate both `SUCCESS` and `FAILURE` outcomes directly from the UI to test edge cases and verify automatic seat release.

---

## 21. API Endpoint Documentation

### Authentication (`/api/auth`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register new customer account | Public |
| `POST` | `/api/auth/login` | Authenticate and receive JWT token | Public |
| `GET` | `/api/auth/me` | Fetch authenticated user profile | Authenticated |

### Movies (`/api/movies`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/movies` | Get all active movies | Public |
| `GET` | `/api/movies/{id}` | Get movie details by ID | Public |
| `GET` | `/api/movies/now-showing` | Retrieve currently showing movies | Public |
| `GET` | `/api/movies/upcoming` | Retrieve upcoming releases | Public |
| `GET` | `/api/movies/search?q={query}` | Search movies by title | Public |
| `GET` | `/api/movies/genre/{genre}` | Filter movies by genre | Public |
| `GET` | `/api/movies/language/{lang}` | Filter movies by language | Public |

### Theatres & Screens (`/api/theatres`, `/api/screens`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/theatres` | Get all active theatres | Public |
| `GET` | `/api/theatres/{id}` | Get theatre details | Public |
| `GET` | `/api/theatres/movie/{movieId}` | Find theatres currently showing a movie | Public |
| `GET` | `/api/screens/theatre/{theatreId}` | Get screens in a specific theatre | Public |
| `GET` | `/api/screens/{screenId}/seats` | Get seat configuration for a screen | Public |

### Shows (`/api/shows`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/shows/{id}` | Get show details | Public |
| `GET` | `/api/shows/movie/{movieId}` | Get upcoming shows for a movie | Public |
| `GET` | `/api/shows/movie/{movieId}/dates` | Get available screening dates | Public |
| `GET` | `/api/shows/schedule?movieId=&theatreId=&date=` | Get shows by theatre, movie, and date | Public |
| `GET` | `/api/shows/{showId}/seats` | Fetch dynamic seat layout & live availability | Public |

### Bookings & Tickets (`/api/bookings`, `/api/tickets`, `/api/payments`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/bookings` | Create booking & lock seats | Customer / Admin |
| `GET` | `/api/bookings/my` | View personal booking history | Customer |
| `GET` | `/api/bookings/{id}` | View booking details by ID | Owner / Admin |
| `POST` | `/api/bookings/{id}/cancel` | Cancel booking & release seats | Owner / Admin |
| `POST` | `/api/payments/process` | Simulate payment & confirm booking | Customer / Admin |
| `GET` | `/api/tickets/{reference}` | Retrieve digital ticket by booking reference | Owner / Admin |

### Admin Management (`/api/admin`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/dashboard` | Aggregated metrics & recent bookings | Admin Only |
| `POST` | `/api/admin/movies` | Add new movie | Admin Only |
| `PUT` | `/api/admin/movies/{id}` | Update movie information | Admin Only |
| `DELETE` | `/api/admin/movies/{id}` | Deactivate movie | Admin Only |
| `POST` | `/api/admin/theatres` | Add cinema complex | Admin Only |
| `PUT` | `/api/admin/theatres/{id}` | Update theatre details | Admin Only |
| `POST` | `/api/admin/screens` | Create screen & auto-generate tiered seats | Admin Only |
| `POST` | `/api/admin/shows` | Schedule show with overlap collision check | Admin Only |
| `PUT` | `/api/admin/shows/{id}` | Reschedule / update show | Admin Only |
| `DELETE` | `/api/admin/shows/{id}` | Cancel show | Admin Only |
| `GET` | `/api/admin/bookings` | Audit all customer bookings across system | Admin Only |

---

## 22. Testing Instructions

### Executing Backend Unit Tests
CineBook features an extensive suite of 28 unit tests utilizing JUnit 5 and Mockito, testing authentication, business calculations, double-booking prevention, show collision detection, payment strategies, and cancellations.

Execute tests via Maven wrapper:
```powershell
.\mvnw.cmd test
```

Expected output:
```
[INFO] Running com.movieticket.service.AuthServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.movieticket.service.BookingServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.movieticket.service.CancellationServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.movieticket.service.MovieServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.movieticket.service.PaymentServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.movieticket.service.SeatServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.movieticket.service.ShowServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Executing Automated End-to-End Integration Tests
While the Spring Boot server is running on `http://localhost:8080`, execute the included automated PowerShell test script:

```powershell
powershell -ExecutionPolicy Bypass -File .\test_e2e.ps1
```

This script automatically executes:
1. Customer Registration & Token Issuance.
2. Movie & Theatre Querying from MySQL.
3. Show Selection & Seat Layout Generation.
4. Transactional Seat Hold & Backend Price Calculation.
5. Concurrent Double-Booking Prevention Verification.
6. Multi-Method Payment Simulation & Confirmation.
7. Digital Ticket Verification & Hash Check.
8. Customer Booking Ledger Audit.
9. Admin Dashboard Metrics Computation.
10. Booking Cancellation & Real-Time Seat Release in MySQL.

---

## 23. Object-Oriented Programming (OOP) Concepts Demonstrated

### 1. Encapsulation
* All entity and model fields are declared `private` to safeguard internal state.
* Mutators and accessors (getters/setters) enforce validation rules (e.g., non-negative prices, trimmed strings).
* Critical state changes (e.g. `booking.setBookingStatus(CONFIRMED)`) are restricted to dedicated service methods.

### 2. Abstraction
* Database communication details are hidden behind Spring Data JPA Repository interfaces.
* The frontend interacts only with simplified Data Transfer Objects (DTOs), preventing direct exposure of internal JPA entities.
* The payment processing interface `PaymentProcessor` abstracts provider-specific implementation details away from `PaymentService`.

### 3. Inheritance
* The abstract class `BaseEntity` is inherited by all persistent domain models (`User`, `Movie`, `Theatre`, `Screen`, `Seat`, `Show`, `Booking`, `Payment`).
* Common attributes (`id`, `createdAt`, `updatedAt`) and JPA lifecycle hooks (`@PrePersist`, `@PreUpdate`) are centralized in `BaseEntity`.

### 4. Polymorphism
* **Strategy Design Pattern**: The `PaymentProcessor` interface defines `processPayment(...)`. Concrete classes `CardPaymentProcessor`, `UpiPaymentProcessor`, and `NetBankingPaymentProcessor` dynamically override this method to provide distinct payment simulation logic.
* Runtime polymorphism selects the processor matching `PaymentMethod` dynamically at execution time.

---

## 24. Key Java Classes & Responsibilities for Evaluation

| Class | Package | Responsibility |
| :--- | :--- | :--- |
| `MovieTicketApplication` | `com.movieticket` | Main Spring Boot application entry point and component scanner. |
| `BaseEntity` | `com.movieticket.model` | Abstract superclass providing reusable IDs and audit timestamps. |
| `SecurityConfig` | `com.movieticket.config` | Spring Security filter chain, BCrypt encoder bean, and route authorization. |
| `JwtAuthenticationFilter` | `com.movieticket.security` | Stateless per-request filter validating Bearer JWT tokens in HTTP headers. |
| `GlobalExceptionHandler` | `com.movieticket.exception`| Centralized `@RestControllerAdvice` formatting custom, safe JSON error responses. |
| `BookingService` | `com.movieticket.service` | Core transactional engine managing seat locking, pricing, and cancellations. |
| `ShowService` | `com.movieticket.service` | Schedule validator enforcing zero screen-time overlap for cinema halls. |
| `ScreenService` | `com.movieticket.service` | Screen management and automatic tiered seat layout generator (Regular/Premium/Recliner). |
| `PaymentProcessor` | `...service.payment` | Interface contract defining simulated multi-channel payment execution. |
| `DataInitializer` | `com.movieticket.initializer` | Seed component inserting realistic initial movies, theatres, screens, and shows. |

---

## 25. Future Enhancements
1. **Interactive Seat Selection via WebSockets**: Real-time push updates informing other connected users when a seat is clicked.
2. **Dynamic Surge Pricing**: Automatic price adjustment based on remaining seat percentage and demand.
3. **Food & Beverage (F&B) Add-Ons**: Concession stand ordering integrated into the checkout flow.
4. **Email / SMS Ticket PDF Delivery**: Automatic PDF ticket generation and dispatch via SMTP / Twilio.

---

### Developed for Academic Java Programming Lab Project Evaluation
*Architecture, Backend, Persistence, and Frontend fully verified and operational.*
