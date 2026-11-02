package com.movieticket.controller;

import com.movieticket.os.BookingMetricsCounter;
import com.movieticket.os.TicketNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Unit 3, Unit 5 & Unit 6: Spring Boot Status Controller displaying live Networking (InetAddress, ServerSocket)
 * and Multithreading / Synchronization / Producer-Consumer BoundedBuffer diagnostics.
 */
@RestController
public class HomeController {

    private final BookingMetricsCounter metricsCounter;
    private final TicketNotificationService notificationService;

    @Value("${cinema.socket.server.port:8888}")
    private int socketPort;

    public HomeController(BookingMetricsCounter metricsCounter,
                          TicketNotificationService notificationService) {
        this.metricsCounter = metricsCounter;
        this.notificationService = notificationService;
    }

    @GetMapping(value = "/status", produces = MediaType.TEXT_HTML_VALUE)
    public String getHomeStatus() {
        String hostName = "localhost";
        String hostAddress = "127.0.0.1";
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            hostName = localHost.getHostName();
            hostAddress = localHost.getHostAddress();
        } catch (UnknownHostException ignored) {
        }

        Thread current = Thread.currentThread();
        List<String> recentEvents = notificationService.getRecentNotifications(5);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
        html.append("<title>CineBook - Syllabus Server Status</title>");
        html.append("<style>");
        html.append("body{font-family:'Segoe UI',sans-serif;background:#0b1120;color:#f8fafc;padding:36px 20px;margin:0;}");
        html.append(".card{background:#1e293b;border-radius:14px;padding:30px;max-width:840px;margin:0 auto;border:1px solid #334155;}");
        html.append("h1{color:#38bdf8;margin:0 0 10px 0;}");
        html.append(".badge{display:inline-block;padding:6px 14px;border-radius:20px;font-weight:700;background:#10b981;color:#022c22;font-size:0.85rem;margin-bottom:18px;}");
        html.append("table{width:100%;border-collapse:collapse;margin-top:12px;}");
        html.append("th,td{text-align:left;padding:10px 12px;border-bottom:1px solid #334155;font-size:0.95rem;}");
        html.append("th{color:#94a3b8;width:280px;}");
        html.append(".code{background:#0f172a;padding:12px 16px;border-radius:8px;font-family:Consolas,monospace;color:#38bdf8;margin-top:10px;}");
        html.append("</style></head><body><div class=\"card\">");
        html.append("<h1>CineBook: Movie Ticket Booking System</h1>");
        html.append("<span class=\"badge\">ONLINE - 100% JAVA SYLLABUS COMPLIANT BACKEND</span>");
        html.append("<p>Powered strictly by the 6 Syllabus Units: Java Fundamentals, Collections &amp; Generics, Spring Boot, MySQL &amp; Pure JDBC, Java Networking Sockets, and Multithreading / Synchronization.</p>");

        html.append("<h3>Unit 5 (Networking) &amp; Unit 6 (Multithreading &amp; Synchronization) Live Metrics</h3>");
        html.append("<table>");
        html.append("<tr><th>Local Host (InetAddress)</th><td>").append(hostName).append(" (").append(hostAddress).append(")</td></tr>");
        html.append("<tr><th>TCP Socket Server</th><td>localhost:").append(socketPort).append(" (Active Clients: ")
            .append(metricsCounter.getActiveSocketClients()).append(", Total Connected: ")
            .append(metricsCounter.getTotalSocketConnections()).append(")</td></tr>");
        html.append("<tr><th>Current HTTP Request Thread</th><td>").append(current.getName())
            .append(" [State: ").append(current.getState()).append("]</td></tr>");
        html.append("<tr><th>Notification Consumer Thread</th><td>").append(notificationService.getWorkerThreadName())
            .append(" [State: ").append(notificationService.getWorkerThreadState()).append("]</td></tr>");
        html.append("<tr><th>Producer-Consumer BoundedBuffer</th><td>Pending: ")
            .append(notificationService.getPendingBufferSize()).append(" / ").append(notificationService.getBufferCapacity())
            .append(" | Processed: ").append(notificationService.getProcessedCount()).append("</td></tr>");
        html.append("<tr><th>Synchronized Booking Counters</th><td>Attempts: ")
            .append(metricsCounter.getBookingAttempts()).append(" | Confirmed: ")
            .append(metricsCounter.getConfirmedBookings()).append(" | Cancelled: ")
            .append(metricsCounter.getCancelledBookings()).append("</td></tr>");
        html.append("<tr><th>Deadlock-Free Seat Lock Acquisitions</th><td>")
            .append(metricsCounter.getOrderedLockAcquisitions()).append(" ordered lock acquisitions</td></tr>");
        html.append("</table>");

        html.append("<h3>Recent Producer-Consumer Notification Events</h3><div class=\"code\">");
        if (recentEvents.isEmpty()) {
            html.append("No booking notifications queued yet.");
        } else {
            for (int i = 0; i < recentEvents.size(); i++) {
                html.append(recentEvents.get(i)).append("<br>");
            }
        }
        html.append("</div></div></body></html>");
        return html.toString();
    }
}
