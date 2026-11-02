package com.movieticket.network;

import com.movieticket.os.BookingMetricsCounter;
import com.movieticket.os.TicketNotificationService;
import com.movieticket.service.AuthService;
import com.movieticket.service.BookingService;
import com.movieticket.service.DashboardService;
import com.movieticket.service.MovieService;
import com.movieticket.service.PaymentService;
import com.movieticket.service.SeatService;
import com.movieticket.service.ShowService;
import com.movieticket.service.TheatreService;
import com.movieticket.service.TicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Unit 5 (Networking using Java) & Unit 6 (Java for Operating System - Multithreading):
 * Multi-threaded TCP Socket Server using java.net.ServerSocket, Socket, Runnable, and Thread.
 */
@Component
public class CinemaSocketServer implements CommandLineRunner, Runnable {

    @Value("${cinema.socket.server.port:8888}")
    private int port;

    @Value("${cinema.socket.server.enabled:true}")
    private boolean enabled;

    @Value("${server.port:8080}")
    private int httpPort;

    private final AuthService authService;
    private final MovieService movieService;
    private final TheatreService theatreService;
    private final ShowService showService;
    private final SeatService seatService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final DashboardService dashboardService;
    private final BookingMetricsCounter metricsCounter;
    private final TicketNotificationService notificationService;

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private int workerSequence = 0;

    public CinemaSocketServer(AuthService authService,
                              MovieService movieService,
                              TheatreService theatreService,
                              ShowService showService,
                              SeatService seatService,
                              BookingService bookingService,
                              PaymentService paymentService,
                              TicketService ticketService,
                              DashboardService dashboardService,
                              BookingMetricsCounter metricsCounter,
                              TicketNotificationService notificationService) {
        this.authService = authService;
        this.movieService = movieService;
        this.theatreService = theatreService;
        this.showService = showService;
        this.seatService = seatService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
        this.dashboardService = dashboardService;
        this.metricsCounter = metricsCounter;
        this.notificationService = notificationService;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            System.out.println("[SOCKET] Cinema TCP Socket Server is disabled by configuration.");
            return;
        }

        this.running = true;
        Thread listenerThread = new Thread(this, "CinemaSocketListener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    public void run() {
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("====================================================================");
            System.out.println(">>> Unit 5: Java TCP ServerSocket listening on port: " + port);
            System.out.println(">>> Connect with CinemaClient or: telnet localhost " + port);
            System.out.println("====================================================================");

            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = nextWorkerId();
                    ClientHandler handler = new ClientHandler(
                            clientSocket,
                            httpPort,
                            authService,
                            movieService,
                            theatreService,
                            showService,
                            seatService,
                            bookingService,
                            paymentService,
                            ticketService,
                            dashboardService,
                            metricsCounter,
                            notificationService
                    );
                    Thread clientThread = new Thread(handler, "CinemaClientWorker-" + clientId);
                    clientThread.setDaemon(true);
                    clientThread.start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[SOCKET] Error accepting client socket: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[SOCKET] Could not bind ServerSocket on port " + port + ": " + e.getMessage());
        }
    }

    private synchronized int nextWorkerId() {
        workerSequence = workerSequence + 1;
        return workerSequence;
    }

    public void stopServer() {
        this.running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("[SOCKET] Error closing ServerSocket: " + e.getMessage());
            }
        }
    }
}
