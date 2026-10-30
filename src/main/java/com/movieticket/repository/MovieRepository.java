package com.movieticket.repository;

import com.movieticket.enums.MovieStatus;
import com.movieticket.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByStatus(MovieStatus status);

    List<Movie> findByStatusNot(MovieStatus status);

    List<Movie> findByTitleContainingIgnoreCase(String title);

    List<Movie> findByGenreIgnoreCase(String genre);

    List<Movie> findByLanguageIgnoreCase(String language);

    @Query("SELECT m FROM Movie m WHERE m.status != 'INACTIVE' AND " +
           "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:genre IS NULL OR LOWER(m.genre) = LOWER(:genre)) AND " +
           "(:language IS NULL OR LOWER(m.language) = LOWER(:language))")
    List<Movie> filterMovies(@Param("title") String title,
                             @Param("genre") String genre,
                             @Param("language") String language);

    @Query("SELECT DISTINCT m.genre FROM Movie m WHERE m.status != 'INACTIVE'")
    List<String> findDistinctGenres();

    @Query("SELECT DISTINCT m.language FROM Movie m WHERE m.status != 'INACTIVE'")
    List<String> findDistinctLanguages();
}
