package com.movieticket.controller;

import com.movieticket.dto.ApiResponse;
import com.movieticket.dto.BookingRequest;
import com.movieticket.dto.BookingResponse;
import com.movieticket.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @RequestBody BookingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        BookingResponse response = bookingService.createBooking(request, authHeader);
        return new ResponseEntity<ApiResponse<BookingResponse>>(
                ApiResponse.success("Booking created. Seats held for 10 minutes.", response), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getMyBookings(authHeader)));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @PathVariable String reference,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingByReference(reference, authHeader)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        BookingResponse response = bookingService.cancelBooking(id, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully and seats released.", response));
    }
}
