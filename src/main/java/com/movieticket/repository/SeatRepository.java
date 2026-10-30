package com.movieticket.repository;

import com.movieticket.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(Long screenId);
    List<Seat> findByScreenId(Long screenId);
    List<Seat> findByIdIn(List<Long> seatIds);
    void deleteByScreenId(Long screenId);
}
