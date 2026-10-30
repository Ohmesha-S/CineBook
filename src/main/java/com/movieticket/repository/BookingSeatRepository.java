package com.movieticket.repository;

import com.movieticket.model.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBookingId(Long bookingId);

    List<BookingSeat> findByShowIdAndStatus(Long showId, String status);

    boolean existsByShowIdAndSeatIdAndStatus(Long showId, Long seatId, String status);
}
