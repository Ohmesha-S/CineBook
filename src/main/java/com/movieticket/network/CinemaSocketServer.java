package com.movieticket.network;

import com.movieticket.os.OsSystemInspector;
import com.movieticket.os.OsTicketPrinter;
import com.movieticket.service.*;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Concept 5: Networking using Java & Concept 6: Java OS Concurrency
 * Multi-threaded TCP Socket Server managed by Spring Boot.
 */
@Component
public class CinemaSocketServer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CinemaSocketServer.class);

    @Value("${cinema.socket.server.port:8888}")
    private int port;

    @Value("${cinema.socket.server.enabled:true}")
    private boolean enabled;

    private final AuthService authService;
    private final MovieService movieService;
    private final TheatreService theatreService;
    private final ShowService showService;
    private final SeatService seatService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final DashboardService dashboardService;
    private final OsTicketPrinter osTicketPrinter;
    private final OsSystemInspector osSystemInspector;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    public CinemaSocketServer(AuthService authService,
                              MovieService movieService,
                              TheatreService theatreService,
                              ShowService showService,
                              SeatService seatService,
                              BookingService bookingService,
                              PaymentService paymentService,
                              TicketService ticketService,
                              DashboardService dashboardService,
                              OsTicketPrinter osTicketPrinter,
                              OsSystemInspector osSystemInspector) {
        this.authService = authService;
        this.movieService = movieService;
        this.theatreService = theatreService;
        this.showService = showService;
        this.seatService = seatService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
        this.dashboardService = dashboardService;
        this.osTicketPrinter = osTicketPrinter;
        this.osSystemInspector = osSystemInspector;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            logger.info("Cinema TCP Socket Server is disabled by configuration.");
            return;
        }

        this.threadPool = Executors.newCachedThreadPool();
        this.running = true;

        // Register OS shutdown hook (Concept 6: Java for OS)
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer, "os-shutdown-hook"));

        // Launch TCP ServerSocket in a dedicated background thread
        Thread serverThread = new Thread(this::startServerLoop, "cinema-socket-listener");
        serverThread.setDaemon(false);
        serverThread.start();
    }

    private void startServerLoop() {
        try {
            this.serverSocket = new ServerSocket(port);
            logger.info("====================================================================");
            logger.info(">>> Concept 5: Java TCP Socket Server listening on port: {}", port);
            logger.info(">>> Connect with CinemaClient or: telnet localhost {}", port);
            logger.info("====================================================================");

            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(
                            clientSocket,
                            authService,
                            movieService,
                            theatreService,
                            showService,
                            seatService,
                            bookingService,
                            paymentService,
                            ticketService,
                            dashboardService,
                            osTicketPrinter,
                            osSystemInspector
                    );
                    threadPool.submit(handler);
                } catch (IOException e) {
                    if (running) {
                        logger.error("Error accepting incoming client socket: {}", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Could not bind ServerSocket on port {}: {}", port, e.getMessage());
        }
    }

    @PreDestroy
    public void stopServer() {
        this.running = false;
        logger.info("Shutting down Cinema TCP Socket Server and thread pool...");
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing server socket: {}", e.getMessage());
            }
        }

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Cinema TCP Socket Server stopped successfully.");
    }
}
