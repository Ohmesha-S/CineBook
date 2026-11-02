package com.movieticket.repository;

import com.movieticket.db.DatabaseManager;
import com.movieticket.exception.BookingConflictException;
import com.movieticket.model.Seat;
import com.movieticket.model.Show;
import com.movieticket.model.ShowSeatReservation;
import com.movieticket.model.User;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4: Databases - Pure JDBC Repository for ShowSeatReservation enforcing seat lock uniqueness
 * and temporary hold expiration via SQL DML and transactional Connection passing.
 */
@Repository
public class ShowSeatReservationRepository implements CrudRepository<ShowSeatReservation, Long> {

    private static final String SELECT_JOIN_SEAT =
            "SELECT r.id, r.created_at, r.updated_at, r.show_id, r.seat_id, r.user_id, " +
            "r.booking_reference, r.status, r.expires_at, " +
            "st.row_label, st.seat_number, st.seat_category, st.base_price " +
            "FROM show_seat_reservations r INNER JOIN seats st ON r.seat_id = st.id";

    private final DatabaseManager databaseManager;

    public ShowSeatReservationRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public ShowSeatReservation save(ShowSeatReservation reservation) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return save(con, reservation);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error saving seat reservation: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public ShowSeatReservation save(Connection con, ShowSeatReservation reservation) {
        if (con == null) {
            return save(reservation);
        }
        if (reservation.getId() == null) {
            return insert(con, reservation);
        }
        return update(con, reservation);
    }

    private ShowSeatReservation insert(Connection con, ShowSeatReservation r) {
        r.touchForCreate();
        String sql = "INSERT INTO show_seat_reservations (created_at, updated_at, show_id, seat_id, user_id, " +
                     "booking_reference, status, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(r.getCreatedAt()));
            ps.setTimestamp(2, DatabaseManager.toTimestamp(r.getUpdatedAt()));
            ps.setLong(3, r.getShow().getId());
            ps.setLong(4, r.getSeat().getId());
            ps.setLong(5, r.getUser().getId());
            ps.setString(6, r.getBookingReference());
            ps.setString(7, r.getStatus());
            ps.setTimestamp(8, DatabaseManager.toTimestamp(r.getExpiresAt()));
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                r.setId(keys.getLong(1));
            }
            return r;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || (e.getSQLState() != null && e.getSQLState().startsWith("23"))) {
                throw new BookingConflictException(
                        "One or more selected seats were just reserved by another customer. Please choose different seats.");
            }
            throw new RuntimeException("JDBC error inserting seat reservation: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
        }
    }

    private ShowSeatReservation update(Connection con, ShowSeatReservation r) {
        r.touchForUpdate();
        String sql = "UPDATE show_seat_reservations SET updated_at = ?, user_id = ?, booking_reference = ?, " +
                     "status = ?, expires_at = ? WHERE id = ?";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(r.getUpdatedAt()));
            ps.setLong(2, r.getUser().getId());
            ps.setString(3, r.getBookingReference());
            ps.setString(4, r.getStatus());
            ps.setTimestamp(5, DatabaseManager.toTimestamp(r.getExpiresAt()));
            ps.setLong(6, r.getId());
            ps.executeUpdate();
            return r;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error updating seat reservation: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
        }
    }

    @Override
    public ShowSeatReservation findById(Long id) {
        if (id == null) {
            return null;
        }
        String sql = SELECT_JOIN_SEAT + " WHERE r.id = ?";
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
            throw new RuntimeException("JDBC error finding reservation by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public List<ShowSeatReservation> findAll() {
        String sql = SELECT_JOIN_SEAT + " ORDER BY r.id ASC";
        List<ShowSeatReservation> list = new ArrayList<ShowSeatReservation>();
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
            throw new RuntimeException("JDBC error listing reservations: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public ShowSeatReservation findByShowIdAndSeatId(Long showId, Long seatId) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return findByShowIdAndSeatId(con, showId, seatId);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding seat reservation: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public ShowSeatReservation findByShowIdAndSeatId(Connection con, Long showId, Long seatId) {
        if (con == null) {
            return findByShowIdAndSeatId(showId, seatId);
        }
        String sql = SELECT_JOIN_SEAT + " WHERE r.show_id = ? AND r.seat_id = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setLong(1, showId);
            ps.setLong(2, seatId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding seat reservation: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
        }
    }

    public List<ShowSeatReservation> findByShowId(Long showId) {
        String sql = SELECT_JOIN_SEAT + " WHERE r.show_id = ?";
        List<ShowSeatReservation> list = new ArrayList<ShowSeatReservation>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, showId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding reservations by show: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<ShowSeatReservation> findByBookingReference(String bookingReference) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return findByBookingReference(con, bookingReference);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding reservations by bookingReference: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<ShowSeatReservation> findByBookingReference(Connection con, String bookingReference) {
        if (con == null) {
            return findByBookingReference(bookingReference);
        }
        String sql = SELECT_JOIN_SEAT + " WHERE r.booking_reference = ?";
        List<ShowSeatReservation> list = new ArrayList<ShowSeatReservation>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, bookingReference);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding reservations by bookingReference: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
        }
    }

    public List<ShowSeatReservation> findActiveReservationsByShow(Long showId, LocalDateTime now) {
        String sql = SELECT_JOIN_SEAT +
                " WHERE r.show_id = ? AND (r.status = 'CONFIRMED' OR (r.status = 'HELD' AND r.expires_at > ?))";
        List<ShowSeatReservation> list = new ArrayList<ShowSeatReservation>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, showId);
            ps.setTimestamp(2, DatabaseManager.toTimestamp(now));
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding active reservations: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public int cleanupExpiredHolds(LocalDateTime now) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return cleanupExpiredHolds(con, now);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error cleaning up expired holds: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public int cleanupExpiredHolds(Connection con, LocalDateTime now) {
        if (con == null) {
            return cleanupExpiredHolds(now);
        }
        String sql = "DELETE FROM show_seat_reservations WHERE status = 'HELD' AND expires_at <= ?";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(now));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error cleaning up expired holds: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
        }
    }

    public int deleteByBookingReference(String bookingReference) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return deleteByBookingReference(con, bookingReference);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting reservations by reference: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public int deleteByBookingReference(Connection con, String bookingReference) {
        if (con == null) {
            return deleteByBookingReference(bookingReference);
        }
        String sql = "DELETE FROM show_seat_reservations WHERE booking_reference = ?";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, bookingReference);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting reservations by reference: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
        }
    }

    public int releaseSeats(Long showId, List<Long> seatIds) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return releaseSeats(con, showId, seatIds);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error releasing seats: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public int releaseSeats(Connection con, Long showId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0;
        }
        if (con == null) {
            return releaseSeats(showId, seatIds);
        }
        StringBuilder sql = new StringBuilder("DELETE FROM show_seat_reservations WHERE show_id = ? AND seat_id IN (");
        for (int i = 0; i < seatIds.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");

        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql.toString());
            ps.setLong(1, showId);
            for (int i = 0; i < seatIds.size(); i++) {
                ps.setLong(i + 2, seatIds.get(i));
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error releasing seats: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
        }
    }

    public void delete(ShowSeatReservation reservation) {
        if (reservation != null && reservation.getId() != null) {
            deleteById(reservation.getId());
        }
    }

    public void delete(Connection con, ShowSeatReservation reservation) {
        if (reservation == null || reservation.getId() == null) {
            return;
        }
        if (con == null) {
            deleteById(reservation.getId());
            return;
        }
        String sql = "DELETE FROM show_seat_reservations WHERE id = ?";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setLong(1, reservation.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting reservation: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM show_seat_reservations WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting reservation by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM show_seat_reservations";
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
            throw new RuntimeException("JDBC error counting reservations: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    private ShowSeatReservation mapRow(ResultSet rs) throws SQLException {
        Show show = new Show();
        show.setId(rs.getLong("show_id"));

        Seat seat = new Seat();
        seat.setId(rs.getLong("seat_id"));
        seat.setRowLabel(rs.getString("row_label"));
        seat.setSeatNumber(rs.getInt("seat_number"));
        seat.setSeatCategory(rs.getString("seat_category"));
        seat.setBasePrice(rs.getDouble("base_price"));

        User user = new User();
        user.setId(rs.getLong("user_id"));

        ShowSeatReservation r = new ShowSeatReservation();
        r.setId(rs.getLong("id"));
        r.setCreatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("created_at")));
        r.setUpdatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("updated_at")));
        r.setShow(show);
        r.setSeat(seat);
        r.setUser(user);
        r.setBookingReference(rs.getString("booking_reference"));
        r.setStatus(rs.getString("status"));
        r.setExpiresAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("expires_at")));
        return r;
    }
}
