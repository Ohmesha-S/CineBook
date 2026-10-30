package com.movieticket.controller;

import com.movieticket.dto.ApiResponse;
import com.movieticket.dto.MovieResponse;
import com.movieticket.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAllMovies() {
        return ResponseEntity.ok(ApiResponse.success(movieService.getAllActiveMovies()));
    }

    @GetMapping("/now-showing")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getNowShowing() {
        return ResponseEntity.ok(ApiResponse.success(movieService.getNowShowingMovies()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getUpcoming() {
        return ResponseEntity.ok(ApiResponse.success(movieService.getUpcomingMovies()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getMovieById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> searchMovies(@RequestParam(required = false, defaultValue = "") String title) {
        return ResponseEntity.ok(ApiResponse.success(movieService.searchMovies(title)));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> filterMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(ApiResponse.success(movieService.filterMovies(title, genre, language)));
    }

    @GetMapping("/genres")
    public ResponseEntity<ApiResponse<List<String>>> getGenres() {
        return ResponseEntity.ok(ApiResponse.success(movieService.getAllGenres()));
    }

    @GetMapping("/languages")
    public ResponseEntity<ApiResponse<List<String>>> getLanguages() {
        return ResponseEntity.ok(ApiResponse.success(movieService.getAllLanguages()));
    }
}
