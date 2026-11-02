package com.movieticket.repository;

import com.movieticket.db.DatabaseManager;
import com.movieticket.model.Movie;
import com.movieticket.model.Screen;
import com.movieticket.model.Show;
import com.movieticket.model.Theatre;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4: Databases - Pure JDBC Repository for Show scheduling with 4-table INNER JOIN
 * (shows -> movies, screens, theatres) and conflict detection queries.
 */
@Repository
public class ShowRepository implements CrudRepository<Show, Long> {

    private static final String SELECT_JOIN_ALL =
            "SELECT s.id, s.created_at, s.updated_at, s.movie_id, s.screen_id, s.show_date, " +
            "s.start_time, s.end_time, s.status, s.price_multiplier, " +
            "m.title AS m_title, m.description AS m_desc, m.genre AS m_genre, m.language AS m_lang, " +
            "m.duration_minutes AS m_dur, m.rating AS m_rating, m.poster_url AS m_poster, m.status AS m_status, " +
            "sc.screen_name AS sc_name, sc.total_rows AS sc_rows, sc.total_seats AS sc_seats, " +
            "sc.screen_type AS sc_type, sc.theatre_id AS t_id, " +
            "t.name AS t_name, t.location AS t_location, t.address AS t_address, t.status AS t_status " +
            "FROM shows s " +
            "INNER JOIN movies m ON s.movie_id = m.id " +
            "INNER JOIN screens sc ON s.screen_id = sc.id " +
            "INNER JOIN theatres t ON sc.theatre_id = t.id";

    private final DatabaseManager databaseManager;

    public ShowRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Show save(Show show) {
        if (show.getId() == null) {
            return insert(show);
        }
        return update(show);
    }

    private Show insert(Show show) {
        show.touchForCreate();
        String sql = "INSERT INTO shows (created_at, updated_at, movie_id, screen_id, show_date, " +
                     "start_time, end_time, status, price_multiplier) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(show.getCreatedAt()));
            ps.setTimestamp(2, DatabaseManager.toTimestamp(show.getUpdatedAt()));
            ps.setLong(3, show.getMovie().getId());
            ps.setLong(4, show.getScreen().getId());
            ps.setDate(5, DatabaseManager.toSqlDate(show.getShowDate()));
            ps.setTime(6, DatabaseManager.toSqlTime(show.getStartTime()));
            ps.setTime(7, DatabaseManager.toSqlTime(show.getEndTime()));
            ps.setString(8, show.getStatus());
            ps.setDouble(9, show.getPriceMultiplier());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                show.setId(keys.getLong(1));
            }
            return show;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error inserting show: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Show update(Show show) {
        show.touchForUpdate();
        String sql = "UPDATE shows SET updated_at = ?, movie_id = ?, screen_id = ?, show_date = ?, " +
                     "start_time = ?, end_time = ?, status = ?, price_multiplier = ? WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(show.getUpdatedAt()));
            ps.setLong(2, show.getMovie().getId());
            ps.setLong(3, show.getScreen().getId());
            ps.setDate(4, DatabaseManager.toSqlDate(show.getShowDate()));
            ps.setTime(5, DatabaseManager.toSqlTime(show.getStartTime()));
            ps.setTime(6, DatabaseManager.toSqlTime(show.getEndTime()));
            ps.setString(7, show.getStatus());
            ps.setDouble(8, show.getPriceMultiplier());
            ps.setLong(9, show.getId());
            ps.executeUpdate();
            return show;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error updating show: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public Show findById(Long id) {
        Connection con = null;
        try {
            con = databaseManager.getConnection();
            return findById(con, id);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding show by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public Show findById(Connection con, Long id) {
        if (id == null) {
            return null;
        }
        if (con == null) {
            return findById(id);
        }
        String sql = SELECT_JOIN_ALL + " WHERE s.id = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding show by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
        }
    }

    @Override
    public List<Show> findAll() {
        String sql = SELECT_JOIN_ALL + " ORDER BY s.show_date ASC, s.start_time ASC";
        List<Show> list = new ArrayList<Show>();
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
            throw new RuntimeException("JDBC error listing shows: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Show> findByMovieIdAndStatus(Long movieId, String status) {
        String sql = SELECT_JOIN_ALL + " WHERE s.movie_id = ? AND s.status = ? ORDER BY s.show_date ASC, s.start_time ASC";
        List<Show> list = new ArrayList<Show>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, movieId);
            ps.setString(2, status);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding shows by movie and status: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Show> findUpcomingShowsByMovie(Long movieId, LocalDate today) {
        String sql = SELECT_JOIN_ALL +
                " WHERE s.movie_id = ? AND s.show_date >= ? AND s.status = 'SCHEDULED' " +
                "ORDER BY s.show_date ASC, s.start_time ASC";
        List<Show> list = new ArrayList<Show>();
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
            throw new RuntimeException("JDBC error finding upcoming shows by movie: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Show> findShowsByMovieTheatreAndDate(Long movieId, Long theatreId, LocalDate showDate) {
        String sql = SELECT_JOIN_ALL +
                " WHERE s.movie_id = ? AND sc.theatre_id = ? AND s.show_date = ? AND s.status = 'SCHEDULED' " +
                "ORDER BY s.start_time ASC";
        List<Show> list = new ArrayList<Show>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, movieId);
            ps.setLong(2, theatreId);
            ps.setDate(3, DatabaseManager.toSqlDate(showDate));
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding shows by schedule: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Show> findShowsByTheatre(Long theatreId, LocalDate today) {
        String sql = SELECT_JOIN_ALL +
                " WHERE sc.theatre_id = ? AND s.show_date >= ? AND s.status = 'SCHEDULED' " +
                "ORDER BY s.show_date ASC, s.start_time ASC";
        List<Show> list = new ArrayList<Show>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, theatreId);
            ps.setDate(2, DatabaseManager.toSqlDate(today));
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding shows by theatre: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<LocalDate> findAvailableDatesByMovie(Long movieId, LocalDate today) {
        String sql = "SELECT DISTINCT show_date FROM shows " +
                     "WHERE movie_id = ? AND show_date >= ? AND status = 'SCHEDULED' " +
                     "ORDER BY show_date ASC";
        List<LocalDate> dates = new ArrayList<LocalDate>();
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
                LocalDate d = DatabaseManager.toLocalDate(rs.getDate(1));
                if (d != null) {
                    dates.add(d);
                }
            }
            return dates;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding available show dates: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Show> findConflictingShows(Long screenId, LocalDate showDate,
                                           LocalTime startTime, LocalTime endTime,
                                           Long excludeShowId) {
        StringBuilder sql = new StringBuilder(SELECT_JOIN_ALL +
                " WHERE s.screen_id = ? AND s.show_date = ? AND s.status <> 'CANCELLED' " +
                "AND (s.start_time < ? AND s.end_time > ?)");
        if (excludeShowId != null) {
            sql.append(" AND s.id <> ?");
        }
        List<Show> list = new ArrayList<Show>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql.toString());
            ps.setLong(1, screenId);
            ps.setDate(2, DatabaseManager.toSqlDate(showDate));
            ps.setTime(3, DatabaseManager.toSqlTime(endTime));
            ps.setTime(4, DatabaseManager.toSqlTime(startTime));
            if (excludeShowId != null) {
                ps.setLong(5, excludeShowId);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error checking conflicting shows: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM shows WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting show: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM shows";
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
            throw new RuntimeException("JDBC error counting shows: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public static Show mapRow(ResultSet rs) throws SQLException {
        Movie m = new Movie();
        m.setId(rs.getLong("movie_id"));
        m.setTitle(rs.getString("m_title"));
        m.setDescription(rs.getString("m_desc"));
        m.setGenre(rs.getString("m_genre"));
        m.setLanguage(rs.getString("m_lang"));
        m.setDurationMinutes(rs.getInt("m_dur"));
        m.setRating(rs.getString("m_rating"));
        m.setPosterUrl(rs.getString("m_poster"));
        m.setStatus(rs.getString("m_status"));

        Theatre t = new Theatre();
        t.setId(rs.getLong("t_id"));
        t.setName(rs.getString("t_name"));
        t.setLocation(rs.getString("t_location"));
        t.setAddress(rs.getString("t_address"));
        t.setStatus(rs.getString("t_status"));

        Screen sc = new Screen();
        sc.setId(rs.getLong("screen_id"));
        sc.setTheatre(t);
        sc.setScreenName(rs.getString("sc_name"));
        sc.setTotalRows(rs.getInt("sc_rows"));
        sc.setTotalSeats(rs.getInt("sc_seats"));
        sc.setScreenType(rs.getString("sc_type"));

        Show s = new Show();
        s.setId(rs.getLong("id"));
        s.setCreatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("created_at")));
        s.setUpdatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("updated_at")));
        s.setMovie(m);
        s.setScreen(sc);
        s.setShowDate(DatabaseManager.toLocalDate(rs.getDate("show_date")));
        s.setStartTime(DatabaseManager.toLocalTime(rs.getTime("start_time")));
        s.setEndTime(DatabaseManager.toLocalTime(rs.getTime("end_time")));
        s.setStatus(rs.getString("status"));
        s.setPriceMultiplier(rs.getDouble("price_multiplier"));
        return s;
    }
}
