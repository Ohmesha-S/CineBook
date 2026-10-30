package com.movieticket.os;

import com.movieticket.dto.TicketResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OsTicketPrinterTest {

    @Test
    @DisplayName("Should write formatted ticket receipt to OS file system")
    void testPrintTicketToOs() throws IOException {
        OsTicketPrinter printer = new OsTicketPrinter();
        ReflectionTestUtils.setField(printer, "ticketDir", "target/test-tickets");
        ReflectionTestUtils.setField(printer, "launchViewer", false); // Do not popup notepad in automated test

        TicketResponse ticket = new TicketResponse();
        ticket.setBookingReference("TEST-REF-999");
        ticket.setVerificationCode("VERIFY-123456");
        ticket.setMovieTitle("Inception");
        ticket.setTheatreName("PVR Luxe");
        ticket.setTheatreAddress("Forum Mall, Bangalore");
        ticket.setScreenName("IMAX 3D");
        ticket.setShowDate(LocalDate.now());
        ticket.setStartTime(LocalTime.of(19, 30));
        ticket.setSeatNumbers(List.of("A1", "A2"));
        ticket.setTotalAmount(700.0);
        ticket.setPaymentStatus("COMPLETED");

        Path path = printer.printTicketToOs(ticket);

        assertNotNull(path);
        assertTrue(Files.exists(path));
        String fileContent = Files.readString(path);
        assertTrue(fileContent.contains("TEST-REF-999"));
        assertTrue(fileContent.contains("Inception"));
        assertTrue(fileContent.contains("PVR Luxe"));
        assertTrue(fileContent.contains("A1, A2"));
        assertTrue(fileContent.contains("700.00"));
    }
}
