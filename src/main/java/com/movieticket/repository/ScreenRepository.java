package com.movieticket.repository;

import com.movieticket.db.DatabaseManager;
import com.movieticket.model.Screen;
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
 * Unit 4: Databases - Pure JDBC Repository for Screen with INNER JOIN to Theatre.
 */
@Repository
public class ScreenRepository implements CrudRepository<Screen, Long> {

    private static final String SELECT_JOIN_THEATRE =
            "SELECT sc.id, sc.created_at, sc.updated_at, sc.theatre_id, sc.screen_name, " +
            "sc.total_rows, sc.total_seats, sc.screen_type, " +
            "t.name AS t_name, t.location AS t_location, t.address AS t_address, " +
            "t.contact_number AS t_contact, t.status AS t_status " +
            "FROM screens sc INNER JOIN theatres t ON sc.theatre_id = t.id";

    private final DatabaseManager databaseManager;

    public ScreenRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Screen save(Screen screen) {
        if (screen.getId() == null) {
            return insert(screen);
        }
        return update(screen);
    }

    private Screen insert(Screen screen) {
        screen.touchForCreate();
        String sql = "INSERT INTO screens (created_at, updated_at, theatre_id, screen_name, total_rows, total_seats, screen_type) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(screen.getCreatedAt()));
            ps.setTimestamp(2, DatabaseManager.toTimestamp(screen.getUpdatedAt()));
            ps.setLong(3, screen.getTheatre().getId());
            ps.setString(4, screen.getScreenName());
            ps.setInt(5, screen.getTotalRows() != null ? screen.getTotalRows() : 5);
            ps.setInt(6, screen.getTotalSeats() != null ? screen.getTotalSeats() : 50);
            ps.setString(7, screen.getScreenType());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                screen.setId(keys.getLong(1));
            }
            return screen;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error inserting screen: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Screen update(Screen screen) {
        screen.touchForUpdate();
        String sql = "UPDATE screens SET updated_at = ?, theatre_id = ?, screen_name = ?, " +
                     "total_rows = ?, total_seats = ?, screen_type = ? WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(screen.getUpdatedAt()));
            ps.setLong(2, screen.getTheatre().getId());
            ps.setString(3, screen.getScreenName());
            ps.setInt(4, screen.getTotalRows() != null ? screen.getTotalRows() : 5);
            ps.setInt(5, screen.getTotalSeats() != null ? screen.getTotalSeats() : 50);
            ps.setString(6, screen.getScreenType());
            ps.setLong(7, screen.getId());
            ps.executeUpdate();
            return screen;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error updating screen: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public Screen findById(Long id) {
        if (id == null) {
            return null;
        }
        String sql = SELECT_JOIN_THEATRE + " WHERE sc.id = ?";
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
            throw new RuntimeException("JDBC error finding screen by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public List<Screen> findAll() {
        String sql = SELECT_JOIN_THEATRE + " ORDER BY sc.id ASC";
        List<Screen> list = new ArrayList<Screen>();
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
            throw new RuntimeException("JDBC error listing screens: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Screen> findByTheatreId(Long theatreId) {
        String sql = SELECT_JOIN_THEATRE + " WHERE sc.theatre_id = ? ORDER BY sc.id ASC";
        List<Screen> list = new ArrayList<Screen>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, theatreId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding screens by theatre: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public long countByTheatreId(Long theatreId) {
        String sql = "SELECT COUNT(*) FROM screens WHERE theatre_id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, theatreId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error counting screens by theatre: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM screens WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting screen: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM screens";
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
            throw new RuntimeException("JDBC error counting screens: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Screen mapRow(ResultSet rs) throws SQLException {
        Theatre t = new Theatre();
        t.setId(rs.getLong("theatre_id"));
        t.setName(rs.getString("t_name"));
        t.setLocation(rs.getString("t_location"));
        t.setAddress(rs.getString("t_address"));
        t.setContactNumber(rs.getString("t_contact"));
        t.setStatus(rs.getString("t_status"));

        Screen sc = new Screen();
        sc.setId(rs.getLong("id"));
        sc.setCreatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("created_at")));
        sc.setUpdatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("updated_at")));
        sc.setTheatre(t);
        sc.setScreenName(rs.getString("screen_name"));
        sc.setTotalRows(rs.getInt("total_rows"));
        sc.setTotalSeats(rs.getInt("total_seats"));
        sc.setScreenType(rs.getString("screen_type"));
        return sc;
    }
}
