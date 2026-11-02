package com.movieticket.repository;

import com.movieticket.db.DatabaseManager;
import com.movieticket.model.Booking;
import com.movieticket.model.BookingSeat;
import com.movieticket.model.Seat;
import com.movieticket.model.Show;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4: Databases - Pure JDBC Repository for BookingSeat with transactional Connection support.
 */
@Repository
public class BookingSeatRepository implements CrudRepository<BookingSeat, Long> {

    private static final String SELECT_JOIN_SEAT =
            "SELECT bs.id, bs.created_at, bs.updated_at, bs.booking_id, bs.seat_id, bs.show_id, bs.price, bs.status, " +
            "st.row_label, st.seat_number, st.seat_category, st.base_price, st.active " +
            "FROM booking_seats bs INNER JOIN seats st ON bs.seat_id = st.id";

    private final DatabaseManager databaseManager;

    public BookingSeatRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public BookingSeat save(BookingSeat bookingSeat) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return save(con, bookingSeat);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error saving booking seat: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public BookingSeat save(Connection con, BookingSeat bookingSeat) {
        if (con == null) {
            return save(bookingSeat);
        }
        if (bookingSeat.getId() == null) {
            return insert(con, bookingSeat);
        }
        return update(con, bookingSeat);
    }

    private BookingSeat insert(Connection con, BookingSeat bs) {
        bs.touchForCreate();
        String sql = "INSERT INTO booking_seats (created_at, updated_at, booking_id, seat_id, show_id, price, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(bs.getCreatedAt()));
            ps.setTimestamp(2, DatabaseManager.toTimestamp(bs.getUpdatedAt()));
            ps.setLong(3, bs.getBooking().getId());
            ps.setLong(4, bs.getSeat().getId());
            ps.setLong(5, bs.getShow().getId());
            ps.setDouble(6, bs.getPrice());
            ps.setString(7, bs.getStatus());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                bs.setId(keys.getLong(1));
            }
            return bs;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error inserting booking seat: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
        }
    }

    private BookingSeat update(Connection con, BookingSeat bs) {
        bs.touchForUpdate();
        String sql = "UPDATE booking_seats SET updated_at = ?, price = ?, status = ? WHERE id = ?";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(bs.getUpdatedAt()));
            ps.setDouble(2, bs.getPrice());
            ps.setString(3, bs.getStatus());
            ps.setLong(4, bs.getId());
            ps.executeUpdate();
            return bs;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error updating booking seat: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
        }
    }

    @Override
    public BookingSeat findById(Long id) {
        if (id == null) {
            return null;
        }
        String sql = SELECT_JOIN_SEAT + " WHERE bs.id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding booking seat: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public List<BookingSeat> findAll() {
        String sql = SELECT_JOIN_SEAT + " ORDER BY bs.id ASC";
        List<BookingSeat> list = new ArrayList<BookingSeat>();
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
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error listing booking seats: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<BookingSeat> findByBookingId(Long bookingId) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return findByBookingId(con, bookingId);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding booking seats by bookingId: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<BookingSeat> findByBookingId(Connection con, Long bookingId) {
        if (con == null) {
            return findByBookingId(bookingId);
        }
        String sql = SELECT_JOIN_SEAT + " WHERE bs.booking_id = ? ORDER BY st.row_label ASC, st.seat_number ASC";
        List<BookingSeat> list = new ArrayList<BookingSeat>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setLong(1, bookingId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding booking seats by bookingId: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
        }
    }

    public List<BookingSeat> findByShowIdAndStatus(Long showId, String status) {
        String sql = SELECT_JOIN_SEAT + " WHERE bs.show_id = ? AND bs.status = ?";
        List<BookingSeat> list = new ArrayList<BookingSeat>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, showId);
            ps.setString(2, status);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding booking seats by show and status: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public boolean existsByShowIdAndSeatIdAndStatus(Long showId, Long seatId, String status) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return existsByShowIdAndSeatIdAndStatus(con, showId, seatId, status);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error checking seat booking status: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public boolean existsByShowIdAndSeatIdAndStatus(Connection con, Long showId, Long seatId, String status) {
        if (con == null) {
            return existsByShowIdAndSeatIdAndStatus(showId, seatId, status);
        }
        String sql = "SELECT COUNT(*) FROM booking_seats WHERE show_id = ? AND seat_id = ? AND status = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setLong(1, showId);
            ps.setLong(2, seatId);
            ps.setString(3, status);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1) > 0L;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error checking seat booking status: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM booking_seats WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting booking seat: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM booking_seats";
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
            throw new RuntimeException("JDBC error counting booking seats: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    private BookingSeat mapRow(ResultSet rs) throws SQLException {
        Seat st = new Seat();
        st.setId(rs.getLong("seat_id"));
        st.setRowLabel(rs.getString("row_label"));
        st.setSeatNumber(rs.getInt("seat_number"));
        st.setSeatCategory(rs.getString("seat_category"));
        st.setBasePrice(rs.getDouble("base_price"));
        st.setActive(rs.getBoolean("active"));

        Show show = new Show();
        show.setId(rs.getLong("show_id"));

        Booking b = new Booking();
        b.setId(rs.getLong("booking_id"));

        BookingSeat bs = new BookingSeat();
        bs.setId(rs.getLong("id"));
        bs.setCreatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("created_at")));
        bs.setUpdatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("updated_at")));
        bs.setBooking(b);
        bs.setSeat(st);
        bs.setShow(show);
        bs.setPrice(rs.getDouble("price"));
        bs.setStatus(rs.getString("status"));
        return bs;
    }
}
