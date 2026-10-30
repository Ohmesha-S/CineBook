package com.movieticket.controller;

import com.movieticket.dto.ApiResponse;
import com.movieticket.dto.SeatLayoutResponse;
import com.movieticket.dto.ShowResponse;
import com.movieticket.service.SeatService;
import com.movieticket.service.ShowService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService showService;
    private final SeatService seatService;

    public ShowController(ShowService showService, SeatService seatService) {
        this.showService = showService;
        this.seatService = seatService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowResponse>> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(showService.getShowById(id)));
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShowsByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.success(showService.getUpcomingShowsByMovie(movieId)));
    }

    @GetMapping("/theatre/{theatreId}")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShowsByTheatre(@PathVariable Long theatreId) {
        return ResponseEntity.ok(ApiResponse.success(showService.getShowsByTheatre(theatreId)));
    }

    @GetMapping("/movie/{movieId}/dates")
    public ResponseEntity<ApiResponse<List<LocalDate>>> getAvailableDatesForMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.success(showService.getAvailableDatesByMovie(movieId)));
    }

    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShowsBySchedule(
            @RequestParam Long movieId,
            @RequestParam Long theatreId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(showService.getShowsByMovieTheatreAndDate(movieId, theatreId, date)));
    }

    @GetMapping("/{showId}/seats")
    public ResponseEntity<ApiResponse<SeatLayoutResponse>> getSeatLayoutForShow(@PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.success(seatService.getSeatLayoutForShow(showId)));
    }
}
