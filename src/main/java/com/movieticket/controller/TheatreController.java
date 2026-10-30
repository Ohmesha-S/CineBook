package com.movieticket.controller;

import com.movieticket.dto.ApiResponse;
import com.movieticket.dto.TheatreResponse;
import com.movieticket.service.TheatreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
public class TheatreController {

    private final TheatreService theatreService;

    public TheatreController(TheatreService theatreService) {
        this.theatreService = theatreService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> getAllTheatres() {
        return ResponseEntity.ok(ApiResponse.success(theatreService.getAllActiveTheatres()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TheatreResponse>> getTheatreById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(theatreService.getTheatreById(id)));
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> getTheatresForMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.success(theatreService.getTheatresShowingMovie(movieId)));
    }
}
