package com.movieticket.repository;

import com.movieticket.model.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    List<Theatre> findByStatus(String status);

    @Query("SELECT DISTINCT t FROM Show s " +
           "JOIN s.screen sc " +
           "JOIN sc.theatre t " +
           "WHERE s.movie.id = :movieId " +
           "AND s.status = 'SCHEDULED' " +
           "AND s.showDate >= :today " +
           "AND t.status = 'ACTIVE'")
    List<Theatre> findTheatresByMovieAndDate(@Param("movieId") Long movieId, @Param("today") LocalDate today);
}
