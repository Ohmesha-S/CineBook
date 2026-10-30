package com.movieticket.repository;

import com.movieticket.model.ShowSeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowSeatReservationRepository extends JpaRepository<ShowSeatReservation, Long> {

    Optional<ShowSeatReservation> findByShowIdAndSeatId(Long showId, Long seatId);

    List<ShowSeatReservation> findByShowId(Long showId);

    List<ShowSeatReservation> findByBookingReference(String bookingReference);

    @Query("SELECT r FROM ShowSeatReservation r WHERE r.show.id = :showId " +
           "AND (r.status = 'CONFIRMED' OR (r.status = 'HELD' AND r.expiresAt > :now))")
    List<ShowSeatReservation> findActiveReservationsByShow(@Param("showId") Long showId,
                                                           @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM ShowSeatReservation r WHERE r.status = 'HELD' AND r.expiresAt <= :now")
    int cleanupExpiredHolds(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM ShowSeatReservation r WHERE r.bookingReference = :bookingReference")
    int deleteByBookingReference(@Param("bookingReference") String bookingReference);

    @Modifying
    @Query("DELETE FROM ShowSeatReservation r WHERE r.show.id = :showId AND r.seat.id IN :seatIds")
    int releaseSeats(@Param("showId") Long showId, @Param("seatIds") List<Long> seatIds);
}
