package com.movieticket.repository;

import com.movieticket.db.DatabaseManager;
import com.movieticket.model.Movie;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4: Databases - Pure JDBC Repository for Movie catalog queries (SELECT, LIKE, DISTINCT, INSERT, UPDATE, DELETE).
 */
@Repository
public class MovieRepository implements CrudRepository<Movie, Long> {

    private static final String SELECT_COLUMNS =
            "SELECT id, created_at, updated_at, title, description, genre, language, duration_minutes, " +
            "rating, release_date, poster_url, backdrop_url, director, `cast`, status FROM movies";

    private final DatabaseManager databaseManager;

    public MovieRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Movie save(Movie movie) {
        if (movie.getId() == null) {
            return insert(movie);
        }
        return update(movie);
    }

    private Movie insert(Movie movie) {
        movie.touchForCreate();
        String sql = "INSERT INTO movies (created_at, updated_at, title, description, genre, language, " +
                     "duration_minutes, rating, release_date, poster_url, backdrop_url, director, `cast`, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(movie.getCreatedAt()));
            ps.setTimestamp(2, DatabaseManager.toTimestamp(movie.getUpdatedAt()));
            ps.setString(3, movie.getTitle());
            ps.setString(4, movie.getDescription());
            ps.setString(5, movie.getGenre());
            ps.setString(6, movie.getLanguage());
            ps.setInt(7, movie.getDurationMinutes() != null ? movie.getDurationMinutes() : 120);
            ps.setString(8, movie.getRating());
            ps.setDate(9, DatabaseManager.toSqlDate(movie.getReleaseDate()));
            ps.setString(10, movie.getPosterUrl());
            ps.setString(11, movie.getBackdropUrl());
            ps.setString(12, movie.getDirector());
            ps.setString(13, movie.getCast());
            ps.setString(14, movie.getStatus());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                movie.setId(keys.getLong(1));
            }
            return movie;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error inserting movie: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(keys);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    private Movie update(Movie movie) {
        movie.touchForUpdate();
        String sql = "UPDATE movies SET updated_at = ?, title = ?, description = ?, genre = ?, language = ?, " +
                     "duration_minutes = ?, rating = ?, release_date = ?, poster_url = ?, backdrop_url = ?, " +
                     "director = ?, `cast` = ?, status = ? WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setTimestamp(1, DatabaseManager.toTimestamp(movie.getUpdatedAt()));
            ps.setString(2, movie.getTitle());
            ps.setString(3, movie.getDescription());
            ps.setString(4, movie.getGenre());
            ps.setString(5, movie.getLanguage());
            ps.setInt(6, movie.getDurationMinutes() != null ? movie.getDurationMinutes() : 120);
            ps.setString(7, movie.getRating());
            ps.setDate(8, DatabaseManager.toSqlDate(movie.getReleaseDate()));
            ps.setString(9, movie.getPosterUrl());
            ps.setString(10, movie.getBackdropUrl());
            ps.setString(11, movie.getDirector());
            ps.setString(12, movie.getCast());
            ps.setString(13, movie.getStatus());
            ps.setLong(14, movie.getId());
            ps.executeUpdate();
            return movie;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error updating movie: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public Movie findById(Long id) {
        if (id == null) {
            return null;
        }
        String sql = SELECT_COLUMNS + " WHERE id = ?";
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
            throw new RuntimeException("JDBC error finding movie by id: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public List<Movie> findAll() {
        List<Movie> list = new ArrayList<Movie>();
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(SELECT_COLUMNS);
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error finding all movies: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<Movie> findByStatus(String status) {
        String sql = SELECT_COLUMNS + " WHERE status = ?";
        return queryMoviesWithSingleParam(sql, status);
    }

    public List<Movie> findByStatusNot(String status) {
        String sql = SELECT_COLUMNS + " WHERE status <> ?";
        return queryMoviesWithSingleParam(sql, status);
    }

    public List<Movie> findByTitleContainingIgnoreCase(String title) {
        String sql = SELECT_COLUMNS + " WHERE LOWER(title) LIKE LOWER(?)";
        String pattern = "%" + (title != null ? title : "") + "%";
        return queryMoviesWithSingleParam(sql, pattern);
    }

    public List<Movie> findByGenreIgnoreCase(String genre) {
        String sql = SELECT_COLUMNS + " WHERE LOWER(genre) = LOWER(?)";
        return queryMoviesWithSingleParam(sql, genre);
    }

    public List<Movie> findByLanguageIgnoreCase(String language) {
        String sql = SELECT_COLUMNS + " WHERE LOWER(language) = LOWER(?)";
        return queryMoviesWithSingleParam(sql, language);
    }

    public List<Movie> filterMovies(String title, String genre, String language) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS + " WHERE status <> 'INACTIVE'");
        List<String> params = new ArrayList<String>();
        if (title != null && !title.trim().isEmpty()) {
            sql.append(" AND LOWER(title) LIKE LOWER(?)");
            params.add("%" + title.trim() + "%");
        }
        if (genre != null && !genre.trim().isEmpty()) {
            sql.append(" AND LOWER(genre) = LOWER(?)");
            params.add(genre.trim());
        }
        if (language != null && !language.trim().isEmpty()) {
            sql.append(" AND LOWER(language) = LOWER(?)");
            params.add(language.trim());
        }

        List<Movie> list = new ArrayList<Movie>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error filtering movies: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    public List<String> findDistinctGenres() {
        String sql = "SELECT DISTINCT genre FROM movies WHERE status <> 'INACTIVE' ORDER BY genre ASC";
        return queryStringColumn(sql);
    }

    public List<String> findDistinctLanguages() {
        String sql = "SELECT DISTINCT language FROM movies WHERE status <> 'INACTIVE' ORDER BY language ASC";
        return queryStringColumn(sql);
    }

    private List<Movie> queryMoviesWithSingleParam(String sql, String param) {
        List<Movie> list = new ArrayList<Movie>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, param);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error querying movies: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    private List<String> queryStringColumn(String sql) {
        List<String> list = new ArrayList<String>();
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            con = databaseManager.getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String val = rs.getString(1);
                if (val != null) {
                    list.add(val);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error querying distinct values: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM movies WHERE id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = databaseManager.getConnection();
            ps = con.prepareStatement(sql);
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC error deleting movie: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(ps);
            DatabaseManager.closeQuietly(con);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM movies";
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
            throw new RuntimeException("JDBC error counting movies: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(rs);
            DatabaseManager.closeQuietly(stmt);
            DatabaseManager.closeQuietly(con);
        }
    }

    public static Movie mapRow(ResultSet rs) throws SQLException {
        Movie m = new Movie();
        m.setId(rs.getLong("id"));
        m.setCreatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("created_at")));
        m.setUpdatedAt(DatabaseManager.toLocalDateTime(rs.getTimestamp("updated_at")));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        m.setGenre(rs.getString("genre"));
        m.setLanguage(rs.getString("language"));
        m.setDurationMinutes(rs.getInt("duration_minutes"));
        m.setRating(rs.getString("rating"));
        m.setReleaseDate(DatabaseManager.toLocalDate(rs.getDate("release_date")));
        m.setPosterUrl(rs.getString("poster_url"));
        m.setBackdropUrl(rs.getString("backdrop_url"));
        m.setDirector(rs.getString("director"));
        m.setCast(rs.getString("cast"));
        m.setStatus(rs.getString("status"));
        return m;
    }
}
