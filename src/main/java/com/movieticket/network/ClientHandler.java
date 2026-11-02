package com.movieticket.network;

import com.movieticket.dto.AuthResponse;
import com.movieticket.dto.BookingRequest;
import com.movieticket.dto.BookingResponse;
import com.movieticket.dto.DashboardStatsDto;
import com.movieticket.dto.LoginRequest;
import com.movieticket.dto.MovieRequest;
import com.movieticket.dto.MovieResponse;
import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.RegisterRequest;
import com.movieticket.dto.SeatDto;
import com.movieticket.dto.SeatLayoutResponse;
import com.movieticket.dto.ShowResponse;
import com.movieticket.dto.TicketResponse;
import com.movieticket.enums.MovieStatus;
import com.movieticket.enums.PaymentMethod;
import com.movieticket.enums.UserRole;
import com.movieticket.model.User;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit 5 (Networking using Java - Socket, InetAddress, URL, URLConnection, BufferedReader, PrintWriter)
 * & Unit 6 (Java for OS - Runnable worker thread per connected client).
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final int httpPort;
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

    private User authenticatedUser;

    public ClientHandler(Socket clientSocket,
                         int httpPort,
                         AuthService authService,
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
        this.clientSocket = clientSocket;
        this.httpPort = httpPort;
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
    public void run() {
        metricsCounter.registerSocketClientConnected();
        String clientIp = String.valueOf(clientSocket.getRemoteSocketAddress());
        System.out.println("[SOCKET] Client connected on " + Thread.currentThread().getName() + " from " + clientIp);

        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);

            out.println("OK Welcome to CineBook Cinema Network Server (Java TCP Sockets Protocol v1.0)");

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
            System.err.println("[SOCKET] Communication ended with " + clientIp + ": " + e.getMessage());
        } finally {
            AuthService.clearCurrentSessionUser();
            metricsCounter.registerSocketClientDisconnected();
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            if (out != null) {
                out.close();
            }
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void processCommand(String line, PrintWriter out) {
        String[] tokens = line.split(" ", 2);
        String command = tokens[0].toUpperCase();
        String args = tokens.length > 1 ? tokens[1].trim() : "";

        try {
            if ("PING".equals(command)) {
                out.println("OK PONG");
            } else if ("HELP".equals(command)) {
                handleHelp(out);
            } else if ("REGISTER".equals(command)) {
                handleRegister(args, out);
            } else if ("LOGIN".equals(command)) {
                handleLogin(args, out);
            } else if ("WHOAMI".equals(command)) {
                handleWhoAmI(out);
            } else if ("MOVIES".equals(command)) {
                handleListMovies(out);
            } else if ("SEARCH".equals(command)) {
                handleSearchMovies(args, out);
            } else if ("SHOWS".equals(command)) {
                handleListShows(args, out);
            } else if ("SEATS".equals(command)) {
                handleViewSeats(args, out);
            } else if ("BOOK".equals(command)) {
                handleBook(args, out);
            } else if ("MY_BOOKINGS".equals(command)) {
                handleMyBookings(out);
            } else if ("CANCEL".equals(command)) {
                handleCancelBooking(args, out);
            } else if ("TICKET".equals(command) || "PRINT_TICKET".equals(command)) {
                handleTicketReceipt(args, out);
            } else if ("THREAD_INFO".equals(command) || "OS_INFO".equals(command)) {
                handleThreadAndNetworkInfo(out);
            } else if ("NET_INFO".equals(command)) {
                handleNetworkClassesDemo(out);
            } else if ("ADMIN_STATS".equals(command)) {
                handleAdminStats(out);
            } else if ("ADMIN_ADD_MOVIE".equals(command)) {
                handleAdminAddMovie(args, out);
            } else {
                out.println("ERROR Unknown command: " + command + ". Type HELP for available commands.");
            }
        } catch (Exception e) {
            out.println("ERROR " + e.getMessage());
        }
    }

    private void handleHelp(PrintWriter out) {
        out.println("DATA_START");
        out.println("Available Commands: PING, MOVIES, SEARCH <kw>, SHOWS <movieId>, SEATS <showId>,");
        out.println("REGISTER <name>|<email>|<phone>|<pass>, LOGIN <email>|<pass>, WHOAMI,");
        out.println("BOOK <showId>|<seatIds>|<CARD/UPI/NET_BANKING>, MY_BOOKINGS, CANCEL <bookingId>,");
        out.println("TICKET <bookingRef>, NET_INFO, THREAD_INFO, ADMIN_STATS, ADMIN_ADD_MOVIE, QUIT");
        out.println("DATA_END");
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
        out.println("OK Registration successful! Logged in as: " + res.getUser().getFullName()
                + " (" + res.getUser().getEmail() + ")");
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
        out.println("OK Login successful! Welcome back, " + res.getUser().getFullName()
                + " [" + res.getUser().getRole() + "]");
    }

    private void handleWhoAmI(PrintWriter out) {
        if (authenticatedUser == null) {
            out.println("OK Not logged in (Guest session). Use LOGIN or REGISTER.");
        } else {
            out.println("OK Logged in as " + authenticatedUser.getFullName()
                    + " (" + authenticatedUser.getEmail() + ") - Role: " + authenticatedUser.getRole());
        }
    }

    private void handleListMovies(PrintWriter out) {
        List<MovieResponse> movies = movieService.getAllActiveMovies();
        out.println("DATA_START");
        out.println("ID | Title | Genre | Language | Duration | Rating");
        out.println("------------------------------------------------------------------");
        for (int i = 0; i < movies.size(); i++) {
            MovieResponse m = movies.get(i);
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
        for (int i = 0; i < movies.size(); i++) {
            MovieResponse m = movies.get(i);
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
        Long movieId = Long.valueOf(Long.parseLong(args.trim()));
        List<ShowResponse> shows = showService.getUpcomingShowsByMovie(movieId);
        out.println("DATA_START");
        out.println("Shows for Movie ID " + movieId + ":");
        out.println("ShowID | Theatre | Screen | Date | Time | Status");
        out.println("------------------------------------------------------------------");
        for (int i = 0; i < shows.size(); i++) {
            ShowResponse s = shows.get(i);
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
        Long showId = Long.valueOf(Long.parseLong(args.trim()));
        SeatLayoutResponse layout = seatService.getSeatLayoutForShow(showId);
        out.println("DATA_START");
        out.println("=== Seat Layout for: " + layout.getTheatreName() + " (" + layout.getScreenName() + ") ===");
        out.println("Total Seats: " + layout.getTotalSeats() + " | Total Rows: " + layout.getTotalRows());
        out.println("------------------------------------------------------------------");

        for (Map.Entry<String, List<SeatDto>> entry : layout.getRows().entrySet()) {
            StringBuilder rowStr = new StringBuilder();
            rowStr.append(String.format("Row %-2s : ", entry.getKey()));
            List<SeatDto> rowSeats = entry.getValue();
            for (int i = 0; i < rowSeats.size(); i++) {
                SeatDto seat = rowSeats.get(i);
                boolean avail = "AVAILABLE".equalsIgnoreCase(seat.getStatus());
                String statusMark = avail
                        ? String.format("[%d:Rs.%.0f]", seat.getId(), seat.getPrice())
                        : "[ X ]";
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

        Long showId = Long.valueOf(Long.parseLong(parts[0].trim()));
        String[] rawSeatTokens = parts[1].trim().split(",");
        List<Long> seatIds = new ArrayList<Long>();
        for (int i = 0; i < rawSeatTokens.length; i++) {
            String token = rawSeatTokens[i].trim();
            if (!token.isEmpty()) {
                seatIds.add(Long.valueOf(Long.parseLong(token)));
            }
        }

        String paymentMethod = parts[2].trim().toUpperCase();
        if (!PaymentMethod.isValid(paymentMethod)) {
            out.println("ERROR Allowed Payment Methods: CARD, UPI, NET_BANKING");
            return;
        }

        AuthService.setCurrentSessionUser(this.authenticatedUser);

        BookingRequest bookingReq = new BookingRequest(showId, seatIds);
        BookingResponse booking = bookingService.createBooking(bookingReq);

        PaymentRequest paymentReq = new PaymentRequest(booking.getBookingReference(), paymentMethod, "SUCCESS");
        paymentService.processPayment(paymentReq);

        TicketResponse ticket = ticketService.getTicket(booking.getBookingReference());

        out.println("OK BOOKING CONFIRMED!");
        out.println("OK Booking Reference: " + ticket.getBookingReference());
        out.println("OK Ticket Number: " + ticket.getTicketNumber() + " (" + ticket.getVerificationCode() + ")");
        out.println("OK Total Paid: Rs. " + ticket.getTotalAmount());
        out.println("OK Seats: " + joinStrings(ticket.getSeatNumbers(), ", "));
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
            for (int i = 0; i < bookings.size(); i++) {
                BookingResponse b = bookings.get(i);
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
        Long bookingId = Long.valueOf(Long.parseLong(args.trim()));
        AuthService.setCurrentSessionUser(this.authenticatedUser);
        BookingResponse res = bookingService.cancelBooking(bookingId);
        out.println("OK Booking " + res.getBookingReference()
                + " successfully cancelled. Seats have been released back to availability.");
    }

    private void handleTicketReceipt(String args, PrintWriter out) {
        if (args.isEmpty()) {
            out.println("ERROR Usage: TICKET <BookingReference>");
            return;
        }
        ensureAuthenticated();
        AuthService.setCurrentSessionUser(this.authenticatedUser);
        TicketResponse ticket = ticketService.getTicket(args.trim());
        out.println("DATA_START");
        out.println("==================================================================");
        out.println("                 CINEBOOK DIGITAL TICKET RECEIPT                  ");
        out.println("==================================================================");
        out.println(" Ticket Number     : " + ticket.getTicketNumber());
        out.println(" Booking Reference : " + ticket.getBookingReference());
        out.println(" Verification Code : " + ticket.getVerificationCode());
        out.println(" Customer          : " + ticket.getCustomerName() + " (" + ticket.getCustomerEmail() + ")");
        out.println(" Movie             : " + ticket.getMovieTitle() + " [" + ticket.getMovieLanguage() + "]");
        out.println(" Theatre           : " + ticket.getTheatreName() + ", " + ticket.getTheatreLocation());
        out.println(" Screen            : " + ticket.getScreenName() + " (" + ticket.getScreenType() + ")");
        out.println(" Show Date & Time  : " + ticket.getShowDate() + " at " + ticket.getStartTime());
        out.println(" Seats             : " + joinStrings(ticket.getSeatNumbers(), ", "));
        out.println(" Amount Paid       : Rs. " + ticket.getTotalAmount() + " [" + ticket.getPaymentStatus() + "]");
        out.println(" Booking Status    : " + ticket.getBookingStatus());
        out.println("==================================================================");
        out.println("DATA_END");
    }

    /**
     * Unit 5 & Unit 6 Diagnostics: Displays InetAddress, URL/URLConnection probe,
     * active worker threads, synchronized counters, Producer-Consumer BoundedBuffer,
     * and Deadlock-Free Ordered Seat Lock statistics.
     */
    private void handleThreadAndNetworkInfo(PrintWriter out) {
        out.println("DATA_START");
        out.println("==================================================================");
        out.println("   UNIT 5 (NETWORKING) & UNIT 6 (MULTITHREADING) DIAGNOSTICS      ");
        out.println("==================================================================");
        try {
            InetAddress local = InetAddress.getLocalHost();
            out.println(" Local HostName (InetAddress)  : " + local.getHostName());
            out.println(" Local IP Address              : " + local.getHostAddress());
        } catch (IOException e) {
            out.println(" Local HostName (InetAddress)  : localhost (127.0.0.1)");
        }
        out.println(" Socket Local Port / Client    : " + clientSocket.getLocalPort()
                + " <-> " + clientSocket.getRemoteSocketAddress());
        out.println(" Current Socket Worker Thread  : " + Thread.currentThread().getName()
                + " [" + Thread.currentThread().getState() + "]");
        out.println(" Active Socket Client Threads  : " + metricsCounter.getActiveSocketClients()
                + " (Total Connected: " + metricsCounter.getTotalSocketConnections() + ")");
        out.println(" Notification Consumer Thread  : " + notificationService.getWorkerThreadName()
                + " [" + notificationService.getWorkerThreadState() + "]");
        out.println(" BoundedBuffer (wait/notifyAll): " + notificationService.getPendingBufferSize()
                + " pending / " + notificationService.getBufferCapacity()
                + " capacity (Processed: " + notificationService.getProcessedCount() + ")");
        out.println(" Synchronized Booking Counters : Attempts=" + metricsCounter.getBookingAttempts()
                + ", Confirmed=" + metricsCounter.getConfirmedBookings()
                + ", Cancelled=" + metricsCounter.getCancelledBookings());
        out.println(" Deadlock-Free Ordered Locks   : " + metricsCounter.getOrderedLockAcquisitions()
                + " sorted seat-lock acquisitions");
        out.println(" Active Theatres Loaded        : " + theatreService.getAllActiveTheatres().size());
        out.println("==================================================================");
        out.println("DATA_END");
    }

    /**
     * Unit 5: Demonstrates java.net.InetAddress, URL, and URLConnection against the local Spring Boot REST endpoint.
     */
    private void handleNetworkClassesDemo(PrintWriter out) {
        out.println("DATA_START");
        out.println("=== Unit 5: Java Network Classes (InetAddress, URL, URLConnection) ===");
        try {
            InetAddress addr = InetAddress.getLocalHost();
            out.println("InetAddress Host Name    : " + addr.getHostName());
            out.println("InetAddress Host Address : " + addr.getHostAddress());

            URL apiUrl = new URL("http://localhost:" + httpPort + "/api/movies/genres");
            out.println("URL Protocol / Host:Port : " + apiUrl.getProtocol() + "://" + apiUrl.getHost() + ":" + apiUrl.getPort());
            out.println("URL Path                 : " + apiUrl.getPath());

            URLConnection connection = apiUrl.openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            out.println("URLConnection ContentType: " + connection.getContentType());
        } catch (IOException e) {
            out.println("Network probe warning    : " + e.getMessage());
        }
        out.println("DATA_END");
    }

    private void handleAdminStats(PrintWriter out) {
        DashboardStatsDto stats = dashboardService.getDashboardStats();
        out.println("DATA_START");
        out.println("==================================================================");
        out.println("                 CINEBOOK PLATFORM ADMINISTRATIVE STATS           ");
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
        req.setDurationMinutes(Integer.valueOf(Integer.parseInt(parts[3].trim())));
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
        if (!UserRole.ADMIN.equals(this.authenticatedUser.getRole())) {
            throw new SecurityException("Administrative privileges required for this operation.");
        }
    }

    private String joinStrings(List<String> items, String delimiter) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
