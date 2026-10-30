package com.movieticket.network;

import com.movieticket.dto.*;
import com.movieticket.enums.*;
import com.movieticket.model.User;
import com.movieticket.os.OsSystemInspector;
import com.movieticket.os.OsTicketPrinter;
import com.movieticket.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Concept 5: Networking using Java & Concept 6: Java OS Concurrency
 * Handles an individual client connection over a TCP Socket using I/O Streams and Multi-threading.
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket clientSocket;
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

    private User authenticatedUser;

    public ClientHandler(Socket clientSocket,
                         AuthService authService,
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
        this.clientSocket = clientSocket;
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
    public void run() {
        String clientIp = clientSocket.getRemoteSocketAddress().toString();
        logger.info("New Java Socket client connected from {}", clientIp);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)) {

            // Welcome banner over TCP socket
            out.println("OK Welcome to CineLuxe Cinema Network Server (Java Sockets Protocol v1.0)");

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase("QUIT") || line.equalsIgnoreCase("EXIT")) {
                    out.println("OK Goodbye! Session terminated.");
                    break;
                }

                if (line.isEmpty()) {
                    continue;
                }

                processCommand(line, out);
            }
        } catch (IOException e) {
            logger.warn("Socket communication error with {}: {}", clientIp, e.getMessage());
        } finally {
            try {
                clientSocket.close();
                logger.info("Socket connection closed for {}", clientIp);
            } catch (IOException e) {
                logger.error("Error closing socket: {}", e.getMessage());
            }
        }
    }

    private void processCommand(String line, PrintWriter out) {
        String[] tokens = line.split(" ", 2);
        String command = tokens[0].toUpperCase();
        String args = tokens.length > 1 ? tokens[1].trim() : "";

        try {
            switch (command) {
                case "PING":
                    out.println("OK PONG");
                    break;

                case "REGISTER":
                    handleRegister(args, out);
                    break;

                case "LOGIN":
                    handleLogin(args, out);
                    break;

                case "WHOAMI":
                    handleWhoAmI(out);
                    break;

                case "MOVIES":
                    handleListMovies(out);
                    break;

                case "SEARCH":
                    handleSearchMovies(args, out);
                    break;

                case "SHOWS":
                    handleListShows(args, out);
                    break;

                case "SEATS":
                    handleViewSeats(args, out);
                    break;

                case "BOOK":
                    handleBook(args, out);
                    break;

                case "MY_BOOKINGS":
                    handleMyBookings(out);
                    break;

                case "CANCEL":
                    handleCancelBooking(args, out);
                    break;

                case "PRINT_TICKET":
                    handlePrintTicket(args, out);
                    break;

                case "OS_INFO":
                    handleOsInfo(out);
                    break;

                case "ADMIN_STATS":
                    handleAdminStats(out);
                    break;

                case "ADMIN_ADD_MOVIE":
                    handleAdminAddMovie(args, out);
                    break;

                default:
                    out.println("ERROR Unknown command: " + command + ". Type HELP for available commands.");
                    break;
            }
        } catch (Exception e) {
            out.println("ERROR " + e.getMessage());
        }
    }

    private void handleRegister(String args, PrintWriter out) {
        String[] parts = args.split("\\|");
        if (parts.length < 4) {
            out.println("ERROR Usage: REGISTER <FullName>|<Email>|<Phone>|<Password>");
            return;
        }
        RegisterRequest req = new RegisterRequest(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
        AuthResponse res = authService.register(req);
        this.authenticatedUser = authService.getCurrentUser(res.getUser().getEmail());
        AuthService.setCurrentSessionUser(this.authenticatedUser);
        out.println("OK Registration successful! Logged in as: " + res.getUser().getFullName() + " (" + res.getUser().getEmail() + ")");
    }

    private void handleLogin(String args, PrintWriter out) {
        String[] parts = args.split("\\|");
        if (parts.length < 2) {
            out.println("ERROR Usage: LOGIN <Email>|<Password>");
            return;
        }
        LoginRequest req = new LoginRequest(parts[0].trim(), parts[1].trim());
        AuthResponse res = authService.login(req);
        this.authenticatedUser = authService.getCurrentUser(res.getUser().getEmail());
        AuthService.setCurrentSessionUser(this.authenticatedUser);
        out.println("OK Login successful! Welcome back, " + res.getUser().getFullName() + " [" + res.getUser().getRole() + "]");
    }

    private void handleWhoAmI(PrintWriter out) {
        if (authenticatedUser == null) {
            out.println("OK Not logged in (Guest session). Use LOGIN or REGISTER.");
        } else {
            out.println("OK Logged in as " + authenticatedUser.getFullName() + " (" + authenticatedUser.getEmail() + ") - Role: " + authenticatedUser.getRole());
        }
    }

    private void handleListMovies(PrintWriter out) {
        List<MovieResponse> movies = movieService.getAllActiveMovies();
        out.println("DATA_START");
        out.println("ID | Title | Genre | Language | Duration | Rating");
        out.println("------------------------------------------------------------------");
        for (MovieResponse m : movies) {
            out.println(String.format("%d | %s | %s | %s | %d mins | %s",
                    m.getId(), m.getTitle(), m.getGenre(), m.getLanguage(), m.getDurationMinutes(), m.getRating()));
        }
        out.println("DATA_END");
    }

    private void handleSearchMovies(String keyword, PrintWriter out) {
        if (keyword.isEmpty()) {
            handleListMovies(out);
            return;
        }
        List<MovieResponse> movies = movieService.searchMovies(keyword);
        out.println("DATA_START");
        out.println("Search Results for: " + keyword);
        out.println("ID | Title | Genre | Language | Duration | Rating");
        out.println("------------------------------------------------------------------");
        for (MovieResponse m : movies) {
            out.println(String.format("%d | %s | %s | %s | %d mins | %s",
                    m.getId(), m.getTitle(), m.getGenre(), m.getLanguage(), m.getDurationMinutes(), m.getRating()));
        }
        out.println("DATA_END");
    }

    private void handleListShows(String args, PrintWriter out) {
        if (args.isEmpty()) {
            out.println("ERROR Usage: SHOWS <MovieId>");
            return;
        }
        Long movieId = Long.parseLong(args.trim());
        List<ShowResponse> shows = showService.getUpcomingShowsByMovie(movieId);
        out.println("DATA_START");
        out.println("Shows for Movie ID " + movieId + ":");
        out.println("ShowID | Theatre | Screen | Date | Time | Status");
        out.println("------------------------------------------------------------------");
        for (ShowResponse s : shows) {
            out.println(String.format("%d | %s | %s | %s | %s | %s",
                    s.getId(), s.getTheatreName(), s.getScreenName(), s.getShowDate(), s.getStartTime(), s.getStatus()));
        }
        out.println("DATA_END");
    }

    private void handleViewSeats(String args, PrintWriter out) {
        if (args.isEmpty()) {
            out.println("ERROR Usage: SEATS <ShowId>");
            return;
        }
        Long showId = Long.parseLong(args.trim());
        SeatLayoutResponse layout = seatService.getSeatLayoutForShow(showId);
        out.println("DATA_START");
        out.println("=== Seat Layout for: " + layout.getTheatreName() + " (" + layout.getScreenName() + ") ===");
        out.println("Total Seats: " + layout.getTotalSeats() + " | Total Rows: " + layout.getTotalRows());
        out.println("------------------------------------------------------------------");

        for (var entry : layout.getRows().entrySet()) {
            StringBuilder rowStr = new StringBuilder();
            rowStr.append(String.format("Row %-2s : ", entry.getKey()));
            for (SeatDto seat : entry.getValue()) {
                boolean avail = "AVAILABLE".equalsIgnoreCase(seat.getStatus());
                String statusMark = avail ? String.format("[%d:Rs.%.0f]", seat.getId(), seat.getPrice()) : "[ X ]";
                rowStr.append(statusMark).append(" ");
            }
            out.println(rowStr.toString());
        }
        out.println("DATA_END");
    }

    private void handleBook(String args, PrintWriter out) {
        ensureAuthenticated();
        String[] parts = args.split("\\|");
        if (parts.length < 3) {
            out.println("ERROR Usage: BOOK <ShowId>|<SeatIdsCommaSeparated>|<PaymentMethod>");
            out.println("ERROR Allowed Payment Methods: CARD, UPI, NET_BANKING");
            return;
        }

        Long showId = Long.parseLong(parts[0].trim());
        List<Long> seatIds = Arrays.stream(parts[1].trim().split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
        PaymentMethod paymentMethod = PaymentMethod.valueOf(parts[2].trim().toUpperCase());

        AuthService.setCurrentSessionUser(this.authenticatedUser);

        // 1. Create temporary reservation
        BookingRequest bookingReq = new BookingRequest(showId, seatIds);
        BookingResponse booking = bookingService.createBooking(bookingReq);

        // 2. Process payment (Strategy Pattern)
        PaymentRequest paymentReq = new PaymentRequest(booking.getBookingReference(), paymentMethod, "SUCCESS");
        PaymentResponse payment = paymentService.processPayment(paymentReq);

        // 3. Generate Ticket
        TicketResponse ticket = ticketService.getTicket(booking.getBookingReference());

        // 4. OS Integration (Concept 6): Print ticket to OS filesystem and launch viewer
        Path ticketFilePath = osTicketPrinter.printTicketToOs(ticket);

        out.println("OK BOOKING CONFIRMED!");
        out.println("OK Booking Reference: " + ticket.getBookingReference());
        out.println("OK Total Paid: Rs. " + ticket.getTotalAmount());
        out.println("OK Seats: " + String.join(", ", ticket.getSeatNumbers()));
        out.println("OK Ticket receipt written to OS filesystem: " + ticketFilePath.toAbsolutePath());
    }

    private void handleMyBookings(PrintWriter out) {
        ensureAuthenticated();
        AuthService.setCurrentSessionUser(this.authenticatedUser);
        List<BookingResponse> bookings = bookingService.getMyBookings();

        out.println("DATA_START");
        out.println("=== Booking History for " + authenticatedUser.getFullName() + " ===");
        if (bookings.isEmpty()) {
            out.println("No bookings found.");
        } else {
            out.println("ID | Booking Ref | Movie | Date & Time | Seats | Total (Rs) | Status");
            out.println("------------------------------------------------------------------");
            for (BookingResponse b : bookings) {
                out.println(String.format("%d | %s | %s | %s %s | %s | %.2f | %s",
                        b.getId(), b.getBookingReference(), b.getMovieTitle(), b.getShowDate(), b.getStartTime(),
                        b.getSeatNumbers(), b.getTotalAmount(), b.getBookingStatus()));
            }
        }
        out.println("DATA_END");
    }

    private void handleCancelBooking(String args, PrintWriter out) {
        ensureAuthenticated();
        if (args.isEmpty()) {
            out.println("ERROR Usage: CANCEL <BookingId>");
            return;
        }
        Long bookingId = Long.parseLong(args.trim());
        AuthService.setCurrentSessionUser(this.authenticatedUser);
        BookingResponse res = bookingService.cancelBooking(bookingId);
        out.println("OK Booking " + res.getBookingReference() + " successfully cancelled. Seats have been released back to availability.");
    }

    private void handlePrintTicket(String args, PrintWriter out) {
        if (args.isEmpty()) {
            out.println("ERROR Usage: PRINT_TICKET <BookingReference>");
            return;
        }
        TicketResponse ticket = ticketService.getTicket(args.trim());
        Path path = osTicketPrinter.printTicketToOs(ticket);
        out.println("OK Ticket receipt generated and opened via OS ProcessBuilder: " + path.toAbsolutePath());
    }

    private void handleOsInfo(PrintWriter out) {
        out.println("DATA_START");
        out.println(osSystemInspector.formatDiagnosticsReport());
        out.println("DATA_END");
    }

    private void handleAdminStats(PrintWriter out) {
        DashboardStatsDto stats = dashboardService.getDashboardStats();
        out.println("DATA_START");
        out.println("==================================================================");
        out.println("                 CINELUXE PLATFORM ADMINISTRATIVE STATS            ");
        out.println("==================================================================");
        out.println(" Total Active Movies   : " + stats.getTotalMovies());
        out.println(" Total Theatres        : " + stats.getTotalTheatres());
        out.println(" Total Screens         : " + stats.getTotalScreens());
        out.println(" Total Active Shows    : " + stats.getTotalShows());
        out.println(" Confirmed Bookings    : " + stats.getConfirmedBookings());
        out.println(" Total Revenue Earned  : Rs. " + stats.getTotalRevenue());
        out.println("==================================================================");
        out.println("DATA_END");
    }

    private void handleAdminAddMovie(String args, PrintWriter out) {
        ensureAdmin();
        String[] parts = args.split("\\|");
        if (parts.length < 6) {
            out.println("ERROR Usage: ADMIN_ADD_MOVIE <Title>|<Genre>|<Language>|<DurationMinutes>|<Rating>|<Description>");
            return;
        }

        MovieRequest req = new MovieRequest();
        req.setTitle(parts[0].trim());
        req.setGenre(parts[1].trim());
        req.setLanguage(parts[2].trim());
        req.setDurationMinutes(Integer.parseInt(parts[3].trim()));
        req.setRating(parts[4].trim());
        req.setDescription(parts[5].trim());
        req.setStatus(MovieStatus.NOW_SHOWING);

        MovieResponse saved = movieService.adminCreateMovie(req);
        out.println("OK Movie added successfully! ID: " + saved.getId() + " - Title: " + saved.getTitle());
    }

    private void ensureAuthenticated() {
        if (this.authenticatedUser == null) {
            this.authenticatedUser = authService.getCurrentAuthenticatedUser();
        }
    }

    private void ensureAdmin() {
        ensureAuthenticated();
        if (this.authenticatedUser.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Administrative privileges required for this operation.");
        }
    }
}
