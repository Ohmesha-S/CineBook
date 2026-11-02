package com.movieticket.controller;

import com.movieticket.dto.ApiResponse;
import com.movieticket.dto.BookingResponse;
import com.movieticket.dto.DashboardStatsDto;
import com.movieticket.dto.MovieRequest;
import com.movieticket.dto.MovieResponse;
import com.movieticket.dto.ScreenRequest;
import com.movieticket.dto.ScreenResponse;
import com.movieticket.dto.ShowRequest;
import com.movieticket.dto.ShowResponse;
import com.movieticket.dto.TheatreRequest;
import com.movieticket.dto.TheatreResponse;
import com.movieticket.service.BookingService;
import com.movieticket.service.DashboardService;
import com.movieticket.service.MovieService;
import com.movieticket.service.ScreenService;
import com.movieticket.service.ShowService;
import com.movieticket.service.TheatreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardStats()));
    }

    @GetMapping("/movies")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAllMovies() {
        return ResponseEntity.ok(ApiResponse.success(movieService.adminGetAllMovies()));
    }

    @PostMapping("/movies")
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@RequestBody MovieRequest request) {
        MovieResponse response = movieService.adminCreateMovie(request);
        return new ResponseEntity<ApiResponse<MovieResponse>>(
                ApiResponse.success("Movie created successfully!", response), HttpStatus.CREATED);
    }

    @PutMapping("/movies/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(@PathVariable Long id,
                                                                  @RequestBody MovieRequest request) {
        MovieResponse response = movieService.adminUpdateMovie(id, request);
        return ResponseEntity.ok(ApiResponse.success("Movie updated successfully!", response));
    }

    @DeleteMapping("/movies/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMovie(@PathVariable Long id) {
        movieService.adminDeleteMovie(id);
        return ResponseEntity.ok(ApiResponse.success("Movie deactivated successfully!"));
    }

    @GetMapping("/theatres")
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> getAllTheatres() {
        return ResponseEntity.ok(ApiResponse.success(theatreService.adminGetAllTheatres()));
    }

    @PostMapping("/theatres")
    public ResponseEntity<ApiResponse<TheatreResponse>> createTheatre(@RequestBody TheatreRequest request) {
        TheatreResponse response = theatreService.adminCreateTheatre(request);
        return new ResponseEntity<ApiResponse<TheatreResponse>>(
                ApiResponse.success("Theatre created successfully!", response), HttpStatus.CREATED);
    }

    @PutMapping("/theatres/{id}")
    public ResponseEntity<ApiResponse<TheatreResponse>> updateTheatre(@PathVariable Long id,
                                                                      @RequestBody TheatreRequest request) {
        TheatreResponse response = theatreService.adminUpdateTheatre(id, request);
        return ResponseEntity.ok(ApiResponse.success("Theatre updated successfully!", response));
    }

    @DeleteMapping("/theatres/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTheatre(@PathVariable Long id) {
        theatreService.adminDeleteTheatre(id);
        return ResponseEntity.ok(ApiResponse.success("Theatre deactivated successfully!"));
    }

    @PostMapping("/screens")
    public ResponseEntity<ApiResponse<ScreenResponse>> createScreen(@RequestBody ScreenRequest request) {
        ScreenResponse response = screenService.createScreen(request);
        return new ResponseEntity<ApiResponse<ScreenResponse>>(
                ApiResponse.success("Screen created and seats generated successfully!", response), HttpStatus.CREATED);
    }

    @PutMapping("/screens/{id}")
    public ResponseEntity<ApiResponse<ScreenResponse>> updateScreen(@PathVariable Long id,
                                                                    @RequestBody ScreenRequest request) {
        ScreenResponse response = screenService.updateScreen(id, request);
        return ResponseEntity.ok(ApiResponse.success("Screen updated successfully!", response));
    }

    @DeleteMapping("/screens/{id}")
    public ResponseEntity<ApiResponse<String>> deleteScreen(@PathVariable Long id) {
        screenService.deleteScreen(id);
        return ResponseEntity.ok(ApiResponse.success("Screen deleted successfully!"));
    }

    @GetMapping("/shows")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getAllShows() {
        return ResponseEntity.ok(ApiResponse.success(showService.adminGetAllShows()));
    }

    @PostMapping("/shows")
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(@RequestBody ShowRequest request) {
        ShowResponse response = showService.adminCreateShow(request);
        return new ResponseEntity<ApiResponse<ShowResponse>>(
                ApiResponse.success("Show scheduled successfully!", response), HttpStatus.CREATED);
    }

    @PutMapping("/shows/{id}")
    public ResponseEntity<ApiResponse<ShowResponse>> updateShow(@PathVariable Long id,
                                                                @RequestBody ShowRequest request) {
        ShowResponse response = showService.adminUpdateShow(id, request);
        return ResponseEntity.ok(ApiResponse.success("Show updated successfully!", response));
    }

    @DeleteMapping("/shows/{id}")
    public ResponseEntity<ApiResponse<String>> cancelShow(@PathVariable Long id) {
        showService.adminCancelShow(id);
        return ResponseEntity.ok(ApiResponse.success("Show cancelled successfully!"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.success(bookingService.adminGetAllBookings()));
    }
}
