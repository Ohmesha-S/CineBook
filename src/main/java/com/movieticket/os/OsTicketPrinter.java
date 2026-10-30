package com.movieticket.os;

import com.movieticket.dto.TicketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Concept 6: Java for Operating System
 * Demonstrates:
 * 1. OS File System interactions via java.nio.file.
 * 2. OS Process Creation & Management using java.lang.ProcessBuilder.
 * 3. Native process inspection using ProcessHandle.
 */
@Component
public class OsTicketPrinter {

    private static final Logger logger = LoggerFactory.getLogger(OsTicketPrinter.class);

    @Value("${cinema.os.ticket.dir:tickets}")
    private String ticketDir;

    @Value("${cinema.os.launch.viewer:true}")
    private boolean launchViewer;

    /**
     * Writes the digital ticket to the Operating System filesystem and invokes
     * the native OS text viewer (e.g. Notepad on Windows) using ProcessBuilder.
     */
    public Path printTicketToOs(TicketResponse ticket) {
        try {
            // 1. OS File System interaction (NIO Path & Directories)
            Path outputDir = Paths.get(ticketDir);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            String filename = "Ticket_" + ticket.getBookingReference() + ".txt";
            Path ticketFile = outputDir.resolve(filename);

            // 2. Format receipt
            String content = buildTicketContent(ticket);
            Files.writeString(ticketFile, content, StandardCharsets.UTF_8);
            logger.info("Ticket receipt persisted to OS filesystem at: {}", ticketFile.toAbsolutePath());

            // 3. Launch OS process using ProcessBuilder
            if (launchViewer) {
                launchNativeOsViewer(ticketFile.toFile());
            }

            return ticketFile;
        } catch (IOException e) {
            logger.error("Failed to write ticket to OS filesystem: {}", e.getMessage(), e);
            throw new RuntimeException("OS filesystem ticket generation failed: " + e.getMessage(), e);
        }
    }

    private String buildTicketContent(TicketResponse ticket) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================================\n");
        sb.append("                 CINELUXE CINEMAS - OFFICIAL E-TICKET               \n");
        sb.append("====================================================================\n");
        sb.append(String.format(" Booking Reference : %s\n", ticket.getBookingReference()));
        sb.append(String.format(" Verification Code : %s\n", ticket.getVerificationCode()));
        sb.append(String.format(" Issued Timestamp  : %s\n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        sb.append("--------------------------------------------------------------------\n");
        sb.append(String.format(" Movie             : %s\n", ticket.getMovieTitle()));
        sb.append(String.format(" Theatre           : %s\n", ticket.getTheatreName()));
        sb.append(String.format(" Address           : %s\n", ticket.getTheatreAddress()));
        sb.append(String.format(" Screen            : %s\n", ticket.getScreenName()));
        sb.append(String.format(" Show Date & Time  : %s at %s\n", ticket.getShowDate(), ticket.getStartTime()));
        sb.append("--------------------------------------------------------------------\n");
        sb.append(String.format(" Seats Reserved    : %s\n", String.join(", ", ticket.getSeatNumbers())));
        sb.append(String.format(" Total Amount Paid : Rs. %.2f\n", ticket.getTotalAmount()));
        sb.append(String.format(" Payment Status    : %s\n", ticket.getPaymentStatus()));
        sb.append("====================================================================\n");
        sb.append(" Please present this digital ticket or printout at the cinema gate. \n");
        sb.append("                  Thank you for booking with CineLuxe!              \n");
        sb.append("====================================================================\n");
        return sb.toString();
    }

    private void launchNativeOsViewer(File file) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            ProcessBuilder processBuilder;

            if (os.contains("win")) {
                // Windows: Launch notepad.exe with the ticket file
                processBuilder = new ProcessBuilder("notepad.exe", file.getAbsolutePath());
            } else if (os.contains("mac")) {
                // macOS: Launch default text viewer
                processBuilder = new ProcessBuilder("open", file.getAbsolutePath());
            } else {
                // Linux / Unix: Launch default text viewer
                processBuilder = new ProcessBuilder("xdg-open", file.getAbsolutePath());
            }

            Process process = processBuilder.start();
            long pid = process.pid();
            logger.info("Successfully spawned OS Process (PID: {}) using ProcessBuilder to view ticket", pid);
        } catch (Exception ex) {
            logger.warn("Could not launch native OS viewer via ProcessBuilder (likely running headless): {}", ex.getMessage());
        }
    }
}
