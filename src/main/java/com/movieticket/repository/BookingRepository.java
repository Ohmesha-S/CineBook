package com.movieticket.repository;

import com.movieticket.db.DatabaseManager;
import com.movieticket.model.Booking;
import com.movieticket.model.BookingSeat;
import com.movieticket.model.Movie;
import com.movieticket.model.Screen;
import com.movieticket.model.Show;
import com.movieticket.model.Theatre;
import com.movieticket.model.User;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4: Databases - Pure JDBC Repository for Booking with multi-table INNER JOINs,
 * aggregate queries (COUNT, SUM), and transactional Connection support.
 */
@Repository
public class BookingRepository implements CrudRepository<Booking, Long> {

    private static final String SELECT_JOIN_ALL =
            "SELECT b.id, b.created_at, b.updated_at, b.booking_reference, b.user_id, b.show_id, " +
            "b.total_amount, b.booking_status, b.booking_time, b.cancellation_time, " +
            "u.full_name AS u_name, u.email AS u_email, u.phone AS u_phone, u.role AS u_role, " +
            "s.show_date AS s_date, s.start_time AS s_start, s.end_time AS s_end, s.status AS s_status, s.price_multiplier AS s_mult, " +
            "s.movie_id AS m_id, m.title AS m_title, m.poster_url AS m_poster, m.genre AS m_genre, " +
            "m.language AS m_lang, m.duration_minutes AS m_dur, " +
            "s.screen_id AS sc_id, sc.screen_name AS sc_name, sc.screen_type AS sc_type, " +
            "sc.theatre_id AS t_id, t.name AS t_name, t.location AS t_location, t.address AS t_address " +
            "FROM bookings b " +
            "INNER JOIN users u ON b.user_id = u.id " +
            "INNER JOIN shows s ON b.show_id = s.id " +
            "INNER JOIN movies m ON s.movie_id = m.id " +
            "INNER JOIN screens sc ON s.screen_id = sc.id " +
            "INNER JOIN theatres t ON sc.theatre_id = t.id";

    private final DatabaseManager databaseManager;
    private final BookingSeatRepository bookingSeatRepository;

    public BookingRepository(DatabaseManager databaseManager, BookingSeatRepository bookingSeatRepository) {
        this.databaseManager = databaseManager;
        this.bookingSeatRepository = bookingSeatRepository;
    }

    @Override
    public Booking save(Booking booking) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return save(con, booking);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error saving booking: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public Booking save(Connection con, Booking booking) {
        if (con == null) {
            return save(booking);
        }
        if (booking.getId() == null) {
            return insert(con, booking);
        }
        return update(con, booking);
    }

    private Booking insert(Connection con, Booking b) {
        b.touchForCreate();
        if (b.getBookingTime() == null) {
            b.setBookingTime(b.getCreatedAt());
        }
        String sql = "INSERT INTO bookings (created_at, updated_at, booking_reference, user_id, show_id, " +
                     "total_amount, booking_status, booking_time, cancellation_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(b.getCreatedAt()));
            ps.setTimestamp(2, DatabaseManager.toTimestamp(b.getUpdatedAt()));
            ps.setString(3, b.getBookingReference());
            ps.setLong(4, b.getUser().getId());
            ps.setLong(5, b.getShow().getId());
            ps.setDouble(6, b.getTotalAmount());
            ps.setString(7, b.getBookingStatus());
            ps.setTimestamp(8, DatabaseManager.toTimestamp(b.getBookingTime()));
            ps.setTimestamp(9, DatabaseManager.toTimestamp(b.getCancellationTime()));
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                b.setId(keys.getLong(1));
            }
            return b;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error inserting booking: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
        }
    }

    private Booking update(Connection con, Booking b) {
        b.touchForUpdate();
        String sql = "UPDATE bookings SET updated_at = ?, total_amount = ?, booking_status = ?, " +
                     "booking_time = ?, cancellation_time = ? WHERE id = ?";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(b.getUpdatedAt()));
            ps.setDouble(2, b.getTotalAmount());
            ps.setString(3, b.getBookingStatus());
            ps.setTimestamp(4, DatabaseManager.toTimestamp(b.getBookingTime()));
            ps.setTimestamp(5, DatabaseManager.toTimestamp(b.getCancellationTime()));
            ps.setLong(6, b.getId());
            ps.executeUpdate();
            return b;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error updating booking: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
        }
    }

    @Override
    public Booking findById(Long id) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return findById(con, id);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding booking by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public Booking findById(Connection con, Long id) {
        if (id == null) {
            return null;
        }
        if (con == null) {
            return findById(id);
        }
        String sql = SELECT_JOIN_ALL + " WHERE b.id = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                Booking b = mapRow(rs);
                hydrateBookingSeats(con, b);
                return b;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding booking by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
        }
    }

    public Booking findByBookingReference(String bookingReference) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return findByBookingReference(con, bookingReference);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding booking by reference: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public Booking findByBookingReference(Connection con, String bookingReference) {
        if (bookingReference == null) {
            return null;
        }
        if (con == null) {
            return findByBookingReference(bookingReference);
        }
        String sql = SELECT_JOIN_ALL + " WHERE b.booking_reference = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, bookingReference);
            rs = ps.executeQuery();
            if (rs.next()) {
                Booking b = mapRow(rs);
                hydrateBookingSeats(con, b);
                return b;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding booking by reference: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
        }
    }

    public List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId) {
        String sql = SELECT_JOIN_ALL + " WHERE b.user_id = ? ORDER BY b.created_at DESC";
        List<Booking> list = new ArrayList<Booking>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            for (Booking b : list) {
                hydrateBookingSeats(con, b);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding bookings by user: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Booking> findAllByOrderByCreatedAtDesc() {
        String sql = SELECT_JOIN_ALL + " ORDER BY b.created_at DESC";
        List<Booking> list = new ArrayList<Booking>();
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            for (Booking b : list) {
                hydrateBookingSeats(con, b);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error listing all bookings: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public List<Booking> findAll() {
        return findAllByOrderByCreatedAtDesc();
    }

    public List<Booking> findByUserAndStatus(Long userId, String status) {
        if (status == null || status.trim().isEmpty()) {
            return findByUserIdOrderByCreatedAtDesc(userId);
        }
        String sql = SELECT_JOIN_ALL + " WHERE b.user_id = ? AND b.booking_status = ? ORDER BY b.created_at DESC";
        List<Booking> list = new ArrayList<Booking>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setString(2, status);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            for (Booking b : list) {
                hydrateBookingSeats(con, b);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding bookings by user and status: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public long countByBookingStatus(String bookingStatus) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE booking_status = ?";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, bookingStatus);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error counting bookings by status: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public Double sumRevenueByStatus(String status) {
        String sql = "SELECT COALESCE(SUM(total_amount), 0.0) FROM bookings WHERE booking_status = ?";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, status);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
            return 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error summing revenue: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM bookings WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting booking: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM bookings";
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error counting bookings: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    private void hydrateBookingSeats(Connection con, Booking booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        List<BookingSeat> seats = bookingSeatRepository.findByBookingId(con, booking.getId());
        for (BookingSeat bs : seats) {
            bs.setBooking(booking);
            bs.setShow(booking.getShow());
        }
        booking.setBookingSeats(seats);
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("user_id"));
        u.setFullName(rs.getString("u_name"));
        u.setEmail(rs.getString("u_email"));
        u.setPhone(rs.getString("u_phone"));
        u.setRole(rs.getString("u_role"));

        Movie m = new Movie();
        m.setId(rs.getLong("m_id"));
        m.setTitle(rs.getString("m_title"));
        m.setPosterUrl(rs.getString("m_poster"));
        m.setGenre(rs.getString("m_genre"));
        m.setLanguage(rs.getString("m_lang"));
        m.setDurationMinutes(rs.getInt("m_dur"));

        Theatre t = new Theatre();
        t.setId(rs.getLong("t_id"));
        t.setName(rs.getString("t_name"));
        t.setLocation(rs.getString("t_location"));
        t.setAddress(rs.getString("t_address"));

        Screen sc = new Screen();
        sc.setId(rs.getLong("sc_id"));
        sc.setTheatre(t);
        sc.setScreenName(rs.getString("sc_name"));
        sc.setScreenType(rs.getString("sc_type"));

        Show s = new Show();
        s.setId(rs.getLong("show_id"));
        s.setMovie(m);
        s.setScreen(sc);
        s.setShowDate(DatabaseManager.toLocalDate(rs.getDate("s_date")));
        s.setStartTime(DatabaseManager.toLocalTime(rs.getTime("s_start")));
        s.setEndTime(DatabaseManager.toLocalTime(rs.getTime("s_end")));
        s.setStatus(rs.getString("s_status"));
        s.setPriceMultiplier(rs.getDouble("s_mult"));

        Booking b = new Booking();
        b.setId(rs.getLong("id"));
        b.setCreatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("created_at")));
        b.setUpdatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("updated_at")));
        b.setBookingReference(rs.getString("booking_reference"));
        b.setUser(u);
        b.setShow(s);
        b.setTotalAmount(rs.getDouble("total_amount"));
        b.setBookingStatus(rs.getString("booking_status"));
        b.setBookingTime(DatabaseManager.toLocalDateTime(rs.getTimestamp("booking_time")));
        b.setCancellationTime(DatabaseManager.toLocalDateTime(rs.getTimestamp("cancellation_time")));
        return b;
    }
}
