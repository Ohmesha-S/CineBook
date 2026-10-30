package com.movieticket.controller;

import com.movieticket.dto.ApiResponse;
import com.movieticket.dto.TicketResponse;
import com.movieticket.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/{bookingReference}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable String bookingReference) {
        TicketResponse response = ticketService.getTicket(bookingReference);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
