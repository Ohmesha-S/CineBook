package com.movieticket.repository;

import com.movieticket.enums.BookingStatus;
import com.movieticket.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findAllByOrderByCreatedAtDesc();

    long countByBookingStatus(BookingStatus bookingStatus);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0.0) FROM Booking b WHERE b.bookingStatus = :status")
    Double sumRevenueByStatus(@Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId " +
           "AND (:status IS NULL OR b.bookingStatus = :status) " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByUserAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status);
}
