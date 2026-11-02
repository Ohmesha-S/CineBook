package com.movieticket.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Unit 4: Databases - JDBC Connection Management, SQL DDL Schema Initialization,
 * and Manual ACID Transaction Helpers using java.sql.DriverManager and Connection.
 */
@Component
public class DatabaseManager {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseManager(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}") String driverClassName) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            System.err.println("[JDBC] MySQL Driver class not found: " + e.getMessage());
        }
    }

    /**
     * Opens a new JDBC Connection to MySQL using DriverManager.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * Executes SQL DDL statements (CREATE TABLE IF NOT EXISTS) for all 10 relational tables.
     */
    public void initializeSchema() {
        String[] ddlStatements = new String[] {
            "CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "full_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL, " +
                "phone VARCHAR(20) NULL, " +
                "password_hash VARCHAR(255) NOT NULL, " +
                "role VARCHAR(20) NOT NULL, " +
                "active BIT(1) NOT NULL DEFAULT 1, " +
                "PRIMARY KEY (id), " +
                "UNIQUE KEY uk_users_email (email)" +
            ")",

            "CREATE TABLE IF NOT EXISTS movies (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "title VARCHAR(150) NOT NULL, " +
                "description TEXT NULL, " +
                "genre VARCHAR(80) NOT NULL, " +
                "language VARCHAR(50) NOT NULL, " +
                "duration_minutes INT NOT NULL, " +
                "rating VARCHAR(10) NULL, " +
                "release_date DATE NULL, " +
                "poster_url VARCHAR(500) NULL, " +
                "backdrop_url VARCHAR(500) NULL, " +
                "director VARCHAR(100) NULL, " +
                "`cast` VARCHAR(255) NULL, " +
                "status VARCHAR(30) NOT NULL, " +
                "PRIMARY KEY (id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS theatres (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "name VARCHAR(120) NOT NULL, " +
                "location VARCHAR(100) NOT NULL, " +
                "address VARCHAR(255) NULL, " +
                "contact_number VARCHAR(30) NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "PRIMARY KEY (id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS screens (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "theatre_id BIGINT NOT NULL, " +
                "screen_name VARCHAR(60) NOT NULL, " +
                "total_rows INT NULL, " +
                "total_seats INT NULL, " +
                "screen_type VARCHAR(50) NULL, " +
                "PRIMARY KEY (id), " +
                "CONSTRAINT fk_screens_theatre FOREIGN KEY (theatre_id) REFERENCES theatres(id) ON DELETE CASCADE" +
            ")",

            "CREATE TABLE IF NOT EXISTS seats (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "screen_id BIGINT NOT NULL, " +
                "row_label VARCHAR(10) NOT NULL, " +
                "seat_number INT NOT NULL, " +
                "seat_category VARCHAR(30) NOT NULL, " +
                "base_price DOUBLE NOT NULL, " +
                "active BIT(1) NOT NULL DEFAULT 1, " +
                "PRIMARY KEY (id), " +
                "UNIQUE KEY uk_screen_row_seat (screen_id, row_label, seat_number), " +
                "CONSTRAINT fk_seats_screen FOREIGN KEY (screen_id) REFERENCES screens(id) ON DELETE CASCADE" +
            ")",

            "CREATE TABLE IF NOT EXISTS shows (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "movie_id BIGINT NOT NULL, " +
                "screen_id BIGINT NOT NULL, " +
                "show_date DATE NOT NULL, " +
                "start_time TIME NOT NULL, " +
                "end_time TIME NOT NULL, " +
                "status VARCHAR(30) NOT NULL, " +
                "price_multiplier DOUBLE NULL DEFAULT 1.0, " +
                "PRIMARY KEY (id), " +
                "CONSTRAINT fk_shows_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_shows_screen FOREIGN KEY (screen_id) REFERENCES screens(id) ON DELETE CASCADE" +
            ")",

            "CREATE TABLE IF NOT EXISTS bookings (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "booking_reference VARCHAR(50) NOT NULL, " +
                "user_id BIGINT NOT NULL, " +
                "show_id BIGINT NOT NULL, " +
                "total_amount DOUBLE NOT NULL, " +
                "booking_status VARCHAR(30) NOT NULL, " +
                "booking_time DATETIME NULL, " +
                "cancellation_time DATETIME NULL, " +
                "PRIMARY KEY (id), " +
                "UNIQUE KEY uk_booking_ref (booking_reference), " +
                "CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id), " +
                "CONSTRAINT fk_bookings_show FOREIGN KEY (show_id) REFERENCES shows(id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS booking_seats (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "booking_id BIGINT NOT NULL, " +
                "seat_id BIGINT NOT NULL, " +
                "show_id BIGINT NOT NULL, " +
                "price DOUBLE NOT NULL, " +
                "status VARCHAR(30) NOT NULL, " +
                "PRIMARY KEY (id), " +
                "CONSTRAINT fk_bseats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_bseats_seat FOREIGN KEY (seat_id) REFERENCES seats(id), " +
                "CONSTRAINT fk_bseats_show FOREIGN KEY (show_id) REFERENCES shows(id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS show_seat_reservations (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "show_id BIGINT NOT NULL, " +
                "seat_id BIGINT NOT NULL, " +
                "user_id BIGINT NOT NULL, " +
                "booking_reference VARCHAR(50) NULL, " +
                "status VARCHAR(30) NOT NULL, " +
                "expires_at DATETIME NOT NULL, " +
                "PRIMARY KEY (id), " +
                "UNIQUE KEY uk_show_seat_reservation (show_id, seat_id), " +
                "CONSTRAINT fk_res_show FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_res_seat FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES users(id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS payments (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NULL, " +
                "booking_id BIGINT NOT NULL, " +
                "payment_reference VARCHAR(60) NOT NULL, " +
                "payment_method VARCHAR(30) NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "payment_status VARCHAR(30) NOT NULL, " +
                "transaction_time DATETIME NULL, " +
                "PRIMARY KEY (id), " +
                "CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE" +
            ")"
        };

        Connection con = null;
        Statement stmt = null;
        try {
            con = getConnection();
            stmt = con.createStatement();
            for (int i = 0; i < ddlStatements.length; i++) {
                stmt.executeUpdate(ddlStatements[i]);
            }
            System.out.println("[JDBC] Verified all 10 MySQL tables successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize MySQL database schema via JDBC: " + e.getMessage(), e);
        } finally {
            closeQuietly(stmt);
            closeQuietly(con);
        }
    }

    public static Timestamp toTimestamp(LocalDateTime dateTime) {
        return (dateTime != null) ? Timestamp.valueOf(dateTime) : null;
    }

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return (timestamp != null) ? timestamp.toLocalDateTime() : null;
    }

    public static Date toSqlDate(LocalDate date) {
        return (date != null) ? Date.valueOf(date) : null;
    }

    public static LocalDate toLocalDate(Date date) {
        return (date != null) ? date.toLocalDate() : null;
    }

    public static Time toSqlTime(LocalTime time) {
        return (time != null) ? Time.valueOf(time) : null;
    }

    public static LocalTime toLocalTime(Time time) {
        return (time != null) ? time.toLocalTime() : null;
    }

    public void rollbackQuietly(Connection con) {
        if (con != null) {
            try {
                if (!con.getAutoCommit()) {
                    con.rollback();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    public static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void closeQuietly(Connection con) {
        if (con != null) {
            try {
                if (!con.getAutoCommit()) {
                    con.setAutoCommit(true);
                }
            } catch (SQLException ignored) {
            }
            try {
                con.close();
            } catch (SQLException ignored) {
            }
        }
    }
}

