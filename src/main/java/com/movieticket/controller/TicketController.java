package com.movieticket.controller;

import com.movieticket.dto.ApiResponse;
import com.movieticket.dto.TicketResponse;
import com.movieticket.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/{bookingReference}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(
            @PathVariable String bookingReference,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TicketResponse response = ticketService.getTicket(bookingReference, authHeader);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
