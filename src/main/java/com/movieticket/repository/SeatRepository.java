package com.movieticket.repository;

import com.movieticket.db.DatabaseManager;
import com.movieticket.model.Screen;
import com.movieticket.model.Seat;
import com.movieticket.model.Theatre;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4: Databases - Pure JDBC Repository for Seat layout persistence, batch insertion, and INNER JOIN queries.
 */
@Repository
public class SeatRepository implements CrudRepository<Seat, Long> {

    private static final String SELECT_JOIN_SCREEN =
            "SELECT st.id, st.created_at, st.updated_at, st.screen_id, st.row_label, st.seat_number, " +
            "st.seat_category, st.base_price, st.active, " +
            "sc.screen_name, sc.total_rows, sc.total_seats, sc.screen_type, sc.theatre_id, " +
            "t.name AS t_name, t.location AS t_location " +
            "FROM seats st " +
            "INNER JOIN screens sc ON st.screen_id = sc.id " +
            "INNER JOIN theatres t ON sc.theatre_id = t.id";

    private final DatabaseManager databaseManager;

    public SeatRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Seat save(Seat seat) {
        if (seat.getId() == null) {
            return insert(seat);
        }
        return update(seat);
    }

    public List<Seat> saveAll(List<Seat> seats) {
        if (seats == null || seats.isEmpty()) {
            return new ArrayList<Seat>();
        }
        String sql = "INSERT INTO seats (created_at, updated_at, screen_id, row_label, seat_number, seat_category, base_price, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            con = databaseManager.getConnection();
            con.setAutoCommit(false);
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (Seat seat : seats) {
                seat.touchForCreate();
                ps.setTimestamp(1, DatabaseManager.toTimestamp(seat.getCreatedAt()));
                ps.setTimestamp(2, DatabaseManager.toTimestamp(seat.getUpdatedAt()));
                ps.setLong(3, seat.getScreen().getId());
                ps.setString(4, seat.getRowLabel());
                ps.setInt(5, seat.getSeatNumber());
                ps.setString(6, seat.getSeatCategory());
                ps.setDouble(7, seat.getBasePrice());
                ps.setBoolean(8, seat.isActive());
                ps.addBatch();
            }
            ps.executeBatch();
            keys = ps.getGeneratedKeys();
            int index = 0;
            while (keys.next() && index < seats.size()) {
                seats.get(index).setId(keys.getLong(1));
                index++;
            }
            con.commit();
            return seats;
        } catch (SQLException e) {
            databaseManager.rollbackQuietly(con);
            throw new RuntimeException("JDBC error batch inserting seats: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Seat insert(Seat seat) {
        seat.touchForCreate();
        String sql = "INSERT INTO seats (created_at, updated_at, screen_id, row_label, seat_number, seat_category, base_price, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(seat.getCreatedAt()));
            ps.setTimestamp(2, DatabaseManager.toTimestamp(seat.getUpdatedAt()));
            ps.setLong(3, seat.getScreen().getId());
            ps.setString(4, seat.getRowLabel());
            ps.setInt(5, seat.getSeatNumber());
            ps.setString(6, seat.getSeatCategory());
            ps.setDouble(7, seat.getBasePrice());
            ps.setBoolean(8, seat.isActive());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                seat.setId(keys.getLong(1));
            }
            return seat;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error inserting seat: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Seat update(Seat seat) {
        seat.touchForUpdate();
        String sql = "UPDATE seats SET updated_at = ?, screen_id = ?, row_label = ?, seat_number = ?, " +
                     "seat_category = ?, base_price = ?, active = ? WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(seat.getUpdatedAt()));
            ps.setLong(2, seat.getScreen().getId());
            ps.setString(3, seat.getRowLabel());
            ps.setInt(4, seat.getSeatNumber());
            ps.setString(5, seat.getSeatCategory());
            ps.setDouble(6, seat.getBasePrice());
            ps.setBoolean(7, seat.isActive());
            ps.setLong(8, seat.getId());
            ps.executeUpdate();
            return seat;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error updating seat: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public Seat findById(Long id) {
        if (id == null) {
            return null;
        }
        String sql = SELECT_JOIN_SCREEN + " WHERE st.id = ?";
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
            throw new RuntimeException("JDBC error finding seat by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public List<Seat> findAll() {
        String sql = SELECT_JOIN_SCREEN + " ORDER BY st.id ASC";
        List<Seat> list = new ArrayList<Seat>();
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
            throw new RuntimeException("JDBC error listing seats: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Seat> findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(Long screenId) {
        String sql = SELECT_JOIN_SCREEN + " WHERE st.screen_id = ? AND st.active = 1 ORDER BY st.row_label ASC, st.seat_number ASC";
        return queryByScreenId(sql, screenId);
    }

    public List<Seat> findByScreenId(Long screenId) {
        String sql = SELECT_JOIN_SCREEN + " WHERE st.screen_id = ? ORDER BY st.row_label ASC, st.seat_number ASC";
        return queryByScreenId(sql, screenId);
    }

    private List<Seat> queryByScreenId(String sql, Long screenId) {
        List<Seat> list = new ArrayList<Seat>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, screenId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error querying seats by screen: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Seat> findByIdIn(List<Long> seatIds) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return findByIdIn(con, seatIds);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding seats by IDs: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Seat> findByIdIn(Connection con, List<Long> seatIds) {
        List<Seat> list = new ArrayList<Seat>();
        if (seatIds == null || seatIds.isEmpty()) {
            return list;
        }
        if (con == null) {
            return findByIdIn(seatIds);
        }
        StringBuilder sql = new StringBuilder(SELECT_JOIN_SCREEN + " WHERE st.id IN (");
        for (int i = 0; i < seatIds.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(") ORDER BY st.row_label ASC, st.seat_number ASC");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql.toString());
            for (int i = 0; i < seatIds.size(); i++) {
                ps.setLong(i + 1, seatIds.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding seats by IDs: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
        }
    }

    public void deleteByScreenId(Long screenId) {
        String sql = "DELETE FROM seats WHERE screen_id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, screenId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting seats by screen: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM seats WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting seat: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM seats";
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
            throw new RuntimeException("JDBC error counting seats: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Seat mapRow(ResultSet rs) throws SQLException {
        Theatre t = new Theatre();
        t.setId(rs.getLong("theatre_id"));
        t.setName(rs.getString("t_name"));
        t.setLocation(rs.getString("t_location"));

        Screen sc = new Screen();
        sc.setId(rs.getLong("screen_id"));
        sc.setTheatre(t);
        sc.setScreenName(rs.getString("screen_name"));
        sc.setTotalRows(rs.getInt("total_rows"));
        sc.setTotalSeats(rs.getInt("total_seats"));
        sc.setScreenType(rs.getString("screen_type"));

        Seat st = new Seat();
        st.setId(rs.getLong("id"));
        st.setCreatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("created_at")));
        st.setUpdatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("updated_at")));
        st.setScreen(sc);
        st.setRowLabel(rs.getString("row_label"));
        st.setSeatNumber(rs.getInt("seat_number"));
        st.setSeatCategory(rs.getString("seat_category"));
        st.setBasePrice(rs.getDouble("base_price"));
        st.setActive(rs.getBoolean("active"));
        return st;
    }
}
