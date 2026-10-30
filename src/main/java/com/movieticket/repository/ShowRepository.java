package com.movieticket.repository;

import com.movieticket.enums.ShowStatus;
import com.movieticket.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieIdAndStatus(Long movieId, ShowStatus status);

    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId " +
           "AND s.showDate >= :today " +
           "AND s.status = 'SCHEDULED' " +
           "ORDER BY s.showDate ASC, s.startTime ASC")
    List<Show> findUpcomingShowsByMovie(@Param("movieId") Long movieId, @Param("today") LocalDate today);

    @Query("SELECT s FROM Show s JOIN s.screen sc " +
           "WHERE s.movie.id = :movieId " +
           "AND sc.theatre.id = :theatreId " +
           "AND s.showDate = :showDate " +
           "AND s.status = 'SCHEDULED' " +
           "ORDER BY s.startTime ASC")
    List<Show> findShowsByMovieTheatreAndDate(@Param("movieId") Long movieId,
                                             @Param("theatreId") Long theatreId,
                                             @Param("showDate") LocalDate showDate);

    @Query("SELECT s FROM Show s JOIN s.screen sc " +
           "WHERE sc.theatre.id = :theatreId " +
           "AND s.showDate >= :today " +
           "AND s.status = 'SCHEDULED' " +
           "ORDER BY s.showDate ASC, s.startTime ASC")
    List<Show> findShowsByTheatre(@Param("theatreId") Long theatreId, @Param("today") LocalDate today);

    @Query("SELECT DISTINCT s.showDate FROM Show s " +
           "WHERE s.movie.id = :movieId " +
           "AND s.showDate >= :today " +
           "AND s.status = 'SCHEDULED' " +
           "ORDER BY s.showDate ASC")
    List<LocalDate> findAvailableDatesByMovie(@Param("movieId") Long movieId, @Param("today") LocalDate today);

    @Query("SELECT s FROM Show s WHERE s.screen.id = :screenId " +
           "AND s.showDate = :showDate " +
           "AND s.status != 'CANCELLED' " +
           "AND (:excludeShowId IS NULL OR s.id != :excludeShowId) " +
           "AND (s.startTime < :endTime AND s.endTime > :startTime)")
    List<Show> findConflictingShows(@Param("screenId") Long screenId,
                                   @Param("showDate") LocalDate showDate,
                                   @Param("startTime") LocalTime startTime,
                                   @Param("endTime") LocalTime endTime,
                                   @Param("excludeShowId") Long excludeShowId);
}
