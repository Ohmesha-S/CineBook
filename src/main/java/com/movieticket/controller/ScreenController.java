package com.movieticket.controller;

import com.movieticket.dto.ApiResponse;
import com.movieticket.dto.ScreenResponse;
import com.movieticket.dto.SeatDto;
import com.movieticket.service.ScreenService;
import com.movieticket.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screens")
public class ScreenController {

    private final ScreenService screenService;
    private final SeatService seatService;

    public ScreenController(ScreenService screenService, SeatService seatService) {
        this.screenService = screenService;
        this.seatService = seatService;
    }

    @GetMapping("/theatre/{theatreId}")
    public ResponseEntity<ApiResponse<List<ScreenResponse>>> getScreensByTheatre(@PathVariable Long theatreId) {
        return ResponseEntity.ok(ApiResponse.success(screenService.getScreensByTheatre(theatreId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScreenResponse>> getScreenById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(screenService.getScreenById(id)));
    }

    @GetMapping("/{screenId}/seats")
    public ResponseEntity<ApiResponse<List<SeatDto>>> getSeatsByScreen(@PathVariable Long screenId) {
        return ResponseEntity.ok(ApiResponse.success(seatService.getSeatsByScreen(screenId)));
    }
}
