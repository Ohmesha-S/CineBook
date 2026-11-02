package com.movieticket.repository;

import com.movieticket.db.DatabaseManager;
import com.movieticket.model.Booking;
import com.movieticket.model.Payment;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4: Databases - Pure JDBC Repository for Payment records with INNER JOIN to Booking
 * and transactional Connection support.
 */
@Repository
public class PaymentRepository implements CrudRepository<Payment, Long> {

    private static final String SELECT_JOIN_BOOKING =
            "SELECT p.id, p.created_at, p.updated_at, p.booking_id, p.payment_reference, " +
            "p.payment_method, p.amount, p.payment_status, p.transaction_time, " +
            "b.booking_reference AS b_ref, b.total_amount AS b_total, b.booking_status AS b_status " +
            "FROM payments p INNER JOIN bookings b ON p.booking_id = b.id";

    private final DatabaseManager databaseManager;

    public PaymentRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Payment save(Payment payment) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return save(con, payment);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error saving payment: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public Payment save(Connection con, Payment payment) {
        if (con == null) {
            return save(payment);
        }
        if (payment.getId() == null) {
            return insert(con, payment);
        }
        return update(con, payment);
    }

    private Payment insert(Connection con, Payment p) {
        p.touchForCreate();
        if (p.getTransactionTime() == null) {
            p.setTransactionTime(p.getCreatedAt());
        }
        String sql = "INSERT INTO payments (created_at, updated_at, booking_id, payment_reference, " +
                     "payment_method, amount, payment_status, transaction_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(p.getCreatedAt()));
            ps.setTimestamp(2, DatabaseManager.toTimestamp(p.getUpdatedAt()));
            ps.setLong(3, p.getBooking().getId());
            ps.setString(4, p.getPaymentReference());
            ps.setString(5, p.getPaymentMethod());
            ps.setDouble(6, p.getAmount());
            ps.setString(7, p.getPaymentStatus());
            ps.setTimestamp(8, DatabaseManager.toTimestamp(p.getTransactionTime()));
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                p.setId(keys.getLong(1));
            }
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error inserting payment: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
        }
    }

    private Payment update(Connection con, Payment p) {
        p.touchForUpdate();
        String sql = "UPDATE payments SET updated_at = ?, payment_method = ?, amount = ?, " +
                     "payment_status = ?, transaction_time = ? WHERE id = ?";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(p.getUpdatedAt()));
            ps.setString(2, p.getPaymentMethod());
            ps.setDouble(3, p.getAmount());
            ps.setString(4, p.getPaymentStatus());
            ps.setTimestamp(5, DatabaseManager.toTimestamp(p.getTransactionTime()));
            ps.setLong(6, p.getId());
            ps.executeUpdate();
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error updating payment: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
        }
    }

    @Override
    public Payment findById(Long id) {
        if (id == null) {
            return null;
        }
        String sql = SELECT_JOIN_BOOKING + " WHERE p.id = ?";
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
            throw new RuntimeException("JDBC error finding payment by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public List<Payment> findAll() {
        String sql = SELECT_JOIN_BOOKING + " ORDER BY p.id DESC";
        List<Payment> list = new ArrayList<Payment>();
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
            throw new RuntimeException("JDBC error listing payments: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Payment> findByBookingId(Long bookingId) {
        String sql = SELECT_JOIN_BOOKING + " WHERE p.booking_id = ? ORDER BY p.id DESC";
        List<Payment> list = new ArrayList<Payment>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, bookingId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding payments by bookingId: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public Payment findByPaymentReference(String paymentReference) {
        if (paymentReference == null) {
            return null;
        }
        String sql = SELECT_JOIN_BOOKING + " WHERE p.payment_reference = ?";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, paymentReference);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding payment by reference: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM payments WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting payment: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM payments";
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
            throw new RuntimeException("JDBC error counting payments: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setId(rs.getLong("booking_id"));
        b.setBookingReference(rs.getString("b_ref"));
        b.setTotalAmount(rs.getDouble("b_total"));
        b.setBookingStatus(rs.getString("b_status"));

        Payment p = new Payment();
        p.setId(rs.getLong("id"));
        p.setCreatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("created_at")));
        p.setUpdatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("updated_at")));
        p.setBooking(b);
        p.setPaymentReference(rs.getString("payment_reference"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setAmount(rs.getDouble("amount"));
        p.setPaymentStatus(rs.getString("payment_status"));
        p.setTransactionTime(DatabaseManager.toLocalDateTime(rs.getTimestamp("transaction_time")));
        return p;
    }
}
