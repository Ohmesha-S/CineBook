package com.movieticket.repository;

import com.movieticket.db.DatabaseManager;
import com.movieticket.model.Theatre;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4: Databases - Pure JDBC Repository for Theatre using LEFT JOIN + GROUP BY + COUNT aggregate
 * and multi-table INNER JOIN queries.
 */
@Repository
public class TheatreRepository implements CrudRepository<Theatre, Long> {

    private static final String SELECT_WITH_SCREEN_COUNT =
            "SELECT t.id, t.created_at, t.updated_at, t.name, t.location, t.address, t.contact_number, t.status, " +
            "COUNT(sc.id) AS screen_count " +
            "FROM theatres t LEFT JOIN screens sc ON sc.theatre_id = t.id ";

    private static final String GROUP_BY_THEATRE =
            " GROUP BY t.id, t.created_at, t.updated_at, t.name, t.location, t.address, t.contact_number, t.status";

    private final DatabaseManager databaseManager;

    public TheatreRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Theatre save(Theatre theatre) {
        if (theatre.getId() == null) {
            return insert(theatre);
        }
        return update(theatre);
    }

    private Theatre insert(Theatre theatre) {
        theatre.touchForCreate();
        String sql = "INSERT INTO theatres (created_at, updated_at, name, location, address, contact_number, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(theatre.getCreatedAt()));
            ps.setTimestamp(2, DatabaseManager.toTimestamp(theatre.getUpdatedAt()));
            ps.setString(3, theatre.getName());
            ps.setString(4, theatre.getLocation());
            ps.setString(5, theatre.getAddress());
            ps.setString(6, theatre.getContactNumber());
            ps.setString(7, theatre.getStatus());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                theatre.setId(keys.getLong(1));
            }
            return theatre;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error inserting theatre: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Theatre update(Theatre theatre) {
        theatre.touchForUpdate();
        String sql = "UPDATE theatres SET updated_at = ?, name = ?, location = ?, address = ?, " +
                     "contact_number = ?, status = ? WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(theatre.getUpdatedAt()));
            ps.setString(2, theatre.getName());
            ps.setString(3, theatre.getLocation());
            ps.setString(4, theatre.getAddress());
            ps.setString(5, theatre.getContactNumber());
            ps.setString(6, theatre.getStatus());
            ps.setLong(7, theatre.getId());
            ps.executeUpdate();
            Theatre refreshed = findById(theatre.getId());
            return refreshed != null ? refreshed : theatre;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error updating theatre: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public Theatre findById(Long id) {
        if (id == null) {
            return null;
        }
        String sql = SELECT_WITH_SCREEN_COUNT + "WHERE t.id = ?" + GROUP_BY_THEATRE;
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
            throw new RuntimeException("JDBC error finding theatre by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public List<Theatre> findAll() {
        String sql = SELECT_WITH_SCREEN_COUNT + GROUP_BY_THEATRE + " ORDER BY t.id ASC";
        List<Theatre> list = new ArrayList<Theatre>();
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
            throw new RuntimeException("JDBC error listing theatres: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Theatre> findByStatus(String status) {
        String sql = SELECT_WITH_SCREEN_COUNT + "WHERE t.status = ?" + GROUP_BY_THEATRE + " ORDER BY t.id ASC";
        List<Theatre> list = new ArrayList<Theatre>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, status);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding theatres by status: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Theatre> findTheatresByMovieAndDate(Long movieId, LocalDate today) {
        String sql = SELECT_WITH_SCREEN_COUNT +
                "WHERE t.status = 'ACTIVE' AND EXISTS (" +
                "SELECT 1 FROM shows s INNER JOIN screens s2 ON s.screen_id = s2.id " +
                "WHERE s2.theatre_id = t.id AND s.movie_id = ? AND s.status = 'SCHEDULED' AND s.show_date >= ?" +
                ")" + GROUP_BY_THEATRE + " ORDER BY t.name ASC";
        List<Theatre> list = new ArrayList<Theatre>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, movieId);
            ps.setDate(2, DatabaseManager.toSqlDate(today));
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding theatres by movie: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM theatres WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting theatre: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM theatres";
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
            throw new RuntimeException("JDBC error counting theatres: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Theatre mapRow(ResultSet rs) throws SQLException {
        Theatre t = new Theatre();
        t.setId(rs.getLong("id"));
        t.setCreatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("created_at")));
        t.setUpdatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("updated_at")));
        t.setName(rs.getString("name"));
        t.setLocation(rs.getString("location"));
        t.setAddress(rs.getString("address"));
        t.setContactNumber(rs.getString("contact_number"));
        t.setStatus(rs.getString("status"));
        t.setScreenCount(rs.getInt("screen_count"));
        return t;
    }
}
