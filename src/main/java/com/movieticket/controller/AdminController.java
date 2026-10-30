package com.movieticket.controller;

import com.movieticket.dto.*;
import com.movieticket.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final DashboardService dashboardService;
    private final MovieService movieService;
    private final TheatreService theatreService;
    private final ScreenService screenService;
    private final ShowService showService;
    private final BookingService bookingService;

    public AdminController(DashboardService dashboardService,
                           MovieService movieService,
                           TheatreService theatreService,
                           ScreenService screenService,
                           ShowService showService,
                           BookingService bookingService) {
        this.dashboardService = dashboardService;
        this.movieService = movieService;
        this.theatreService = theatreService;
        this.screenService = screenService;
        this.showService = showService;
        this.bookingService = bookingService;
    }

    // Dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardStats()));
    }

    // Movie Management
    @GetMapping("/movies")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAllMovies() {
        return ResponseEntity.ok(ApiResponse.success(movieService.adminGetAllMovies()));
    }

    @PostMapping("/movies")
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@Valid @RequestBody MovieRequest request) {
        MovieResponse response = movieService.adminCreateMovie(request);
        return new ResponseEntity<>(ApiResponse.success("Movie created successfully!", response), HttpStatus.CREATED);
    }

    @PutMapping("/movies/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        MovieResponse response = movieService.adminUpdateMovie(id, request);
        return ResponseEntity.ok(ApiResponse.success("Movie updated successfully!", response));
    }

    @DeleteMapping("/movies/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMovie(@PathVariable Long id) {
        movieService.adminDeleteMovie(id);
        return ResponseEntity.ok(ApiResponse.success("Movie deactivated successfully!"));
    }

    // Theatre Management
    @GetMapping("/theatres")
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> getAllTheatres() {
        return ResponseEntity.ok(ApiResponse.success(theatreService.adminGetAllTheatres()));
    }

    @PostMapping("/theatres")
    public ResponseEntity<ApiResponse<TheatreResponse>> createTheatre(@Valid @RequestBody TheatreRequest request) {
        TheatreResponse response = theatreService.adminCreateTheatre(request);
        return new ResponseEntity<>(ApiResponse.success("Theatre created successfully!", response), HttpStatus.CREATED);
    }

    @PutMapping("/theatres/{id}")
    public ResponseEntity<ApiResponse<TheatreResponse>> updateTheatre(@PathVariable Long id, @Valid @RequestBody TheatreRequest request) {
        TheatreResponse response = theatreService.adminUpdateTheatre(id, request);
        return ResponseEntity.ok(ApiResponse.success("Theatre updated successfully!", response));
    }

    @DeleteMapping("/theatres/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTheatre(@PathVariable Long id) {
        theatreService.adminDeleteTheatre(id);
        return ResponseEntity.ok(ApiResponse.success("Theatre deactivated successfully!"));
    }

    // Screen Management
    @PostMapping("/screens")
    public ResponseEntity<ApiResponse<ScreenResponse>> createScreen(@Valid @RequestBody ScreenRequest request) {
        ScreenResponse response = screenService.createScreen(request);
        return new ResponseEntity<>(ApiResponse.success("Screen created and seats generated successfully!", response), HttpStatus.CREATED);
    }

    @PutMapping("/screens/{id}")
    public ResponseEntity<ApiResponse<ScreenResponse>> updateScreen(@PathVariable Long id, @Valid @RequestBody ScreenRequest request) {
        ScreenResponse response = screenService.updateScreen(id, request);
        return ResponseEntity.ok(ApiResponse.success("Screen updated successfully!", response));
    }

    @DeleteMapping("/screens/{id}")
    public ResponseEntity<ApiResponse<String>> deleteScreen(@PathVariable Long id) {
        screenService.deleteScreen(id);
        return ResponseEntity.ok(ApiResponse.success("Screen deleted successfully!"));
    }

    // Show Management
    @GetMapping("/shows")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getAllShows() {
        return ResponseEntity.ok(ApiResponse.success(showService.adminGetAllShows()));
    }

    @PostMapping("/shows")
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(@Valid @RequestBody ShowRequest request) {
        ShowResponse response = showService.adminCreateShow(request);
        return new ResponseEntity<>(ApiResponse.success("Show scheduled successfully!", response), HttpStatus.CREATED);
    }

    @PutMapping("/shows/{id}")
    public ResponseEntity<ApiResponse<ShowResponse>> updateShow(@PathVariable Long id, @Valid @RequestBody ShowRequest request) {
        ShowResponse response = showService.adminUpdateShow(id, request);
        return ResponseEntity.ok(ApiResponse.success("Show updated successfully!", response));
    }

    @DeleteMapping("/shows/{id}")
    public ResponseEntity<ApiResponse<String>> cancelShow(@PathVariable Long id) {
        showService.adminCancelShow(id);
        return ResponseEntity.ok(ApiResponse.success("Show cancelled successfully!"));
    }

    // Booking Monitoring
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.success(bookingService.adminGetAllBookings()));
    }
}
