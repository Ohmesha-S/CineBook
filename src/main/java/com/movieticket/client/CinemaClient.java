package com.movieticket.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Concept 5: Networking using Java
 * Standalone Interactive Java Console Client connecting to the Cinema Server via TCP Sockets.
 * Run via: java -cp target/classes com.movieticket.client.CinemaClient
 */
public class CinemaClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8888;

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Scanner scanner;

    public CinemaClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        CinemaClient client = new CinemaClient(host, port);
        client.start();
    }

    public void start() {
        System.out.println("====================================================================");
        System.out.println("            CINELUXE CINEMAS - JAVA CONSOLE CLIENT                  ");
        System.out.println("               (Networking using Java TCP Sockets)                  ");
        System.out.println("====================================================================");
        System.out.println("Connecting to Cinema Server at " + host + ":" + port + "...");

        try {
            this.socket = new Socket(host, port);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Read welcome message from server
            String welcome = in.readLine();
            System.out.println(welcome);
            System.out.println();

            runMenuLoop();

        } catch (IOException e) {
            System.err.println("Could not connect to Cinema Server at " + host + ":" + port);
            System.err.println("Reason: " + e.getMessage());
            System.err.println("Please make sure the Spring Boot server is running: .\\mvnw.cmd spring-boot:run");
        } finally {
            closeConnection();
        }
    }

    private void runMenuLoop() {
        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print("Enter choice (1-12): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    browseMovies();
                    break;
                case "2":
                    searchMovies();
                    break;
                case "3":
                    viewShows();
                    break;
                case "4":
                    viewSeats();
                    break;
                case "5":
                    bookSeats();
                    break;
                case "6":
                    viewMyBookings();
                    break;
                case "7":
                    cancelBooking();
                    break;
                case "8":
                    registerUser();
                    break;
                case "9":
                    loginUser();
                    break;
                case "10":
                    adminOperations();
                    break;
                case "11":
                    showOsDiagnostics();
                    break;
                case "12":
                case "exit":
                case "quit":
                    sendCommand("QUIT");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 12.");
                    break;
            }
            System.out.println();
        }
    }

    private void printMainMenu() {
        System.out.println("--------------------------------------------------------------------");
        System.out.println(" [1] Browse Now Showing Movies");
        System.out.println(" [2] Search Movies by Keyword");
        System.out.println(" [3] View Shows for a Movie");
        System.out.println(" [4] View Seat Layout & Pricing");
        System.out.println(" [5] Book Movie Tickets & Launch OS Viewer");
        System.out.println(" [6] View My Booking History");
        System.out.println(" [7] Cancel a Booking");
        System.out.println(" [8] Register New Customer");
        System.out.println(" [9] Customer / Admin Login");
        System.out.println(" [10] Admin Operations (Stats & Add Movie)");
        System.out.println(" [11] OS & Hardware Diagnostics (Concept 6: Java OS)");
        System.out.println(" [12] Exit");
        System.out.println("--------------------------------------------------------------------");
    }

    private void browseMovies() {
        sendCommand("MOVIES");
        readAndPrintBlockResponse();
    }

    private void searchMovies() {
        System.out.print("Enter movie title or genre keyword to search: ");
        String keyword = scanner.nextLine().trim();
        sendCommand("SEARCH " + keyword);
        readAndPrintBlockResponse();
    }

    private void viewShows() {
        System.out.print("Enter Movie ID: ");
        String movieId = scanner.nextLine().trim();
        sendCommand("SHOWS " + movieId);
        readAndPrintBlockResponse();
    }

    private void viewSeats() {
        System.out.print("Enter Show ID: ");
        String showId = scanner.nextLine().trim();
        sendCommand("SEATS " + showId);
        readAndPrintBlockResponse();
    }

    private void bookSeats() {
        System.out.println("=== Book Movie Tickets ===");
        System.out.print("Enter Show ID: ");
        String showId = scanner.nextLine().trim();

        System.out.print("Enter Seat ID(s) comma-separated (e.g. 1,2): ");
        String seatIds = scanner.nextLine().trim();

        System.out.print("Select Payment Method (CARD / UPI / NET_BANKING): ");
        String paymentMethod = scanner.nextLine().trim().toUpperCase();

        sendCommand("BOOK " + showId + "|" + seatIds + "|" + paymentMethod);
        readAndPrintSingleOrMultiResponse();
    }

    private void viewMyBookings() {
        sendCommand("MY_BOOKINGS");
        readAndPrintBlockResponse();
    }

    private void cancelBooking() {
        System.out.print("Enter Booking ID to cancel: ");
        String bookingId = scanner.nextLine().trim();
        sendCommand("CANCEL " + bookingId);
        readAndPrintSingleOrMultiResponse();
    }

    private void registerUser() {
        System.out.println("=== Customer Registration ===");
        System.out.print("Enter Full Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter Email Address: ");
        String email = scanner.nextLine().trim();

        System.out.print("Enter Phone Number: ");
        String phone = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        sendCommand("REGISTER " + name + "|" + email + "|" + phone + "|" + password);
        readAndPrintSingleOrMultiResponse();
    }

    private void loginUser() {
        System.out.println("=== User Login ===");
        System.out.print("Enter Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        sendCommand("LOGIN " + email + "|" + password);
        readAndPrintSingleOrMultiResponse();
    }

    private void adminOperations() {
        System.out.println("--- Admin Operations ---");
        System.out.println("1. View Platform Statistics");
        System.out.println("2. Add New Movie");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();

        if ("1".equals(choice)) {
            sendCommand("ADMIN_STATS");
            readAndPrintBlockResponse();
        } else if ("2".equals(choice)) {
            System.out.print("Enter Title: ");
            String title = scanner.nextLine().trim();

            System.out.print("Enter Genre (ACTION/DRAMA/COMEDY/SCI_FI/HORROR/ROMANCE/THRILLER/ANIMATION): ");
            String genre = scanner.nextLine().trim().toUpperCase();

            System.out.print("Enter Language (ENGLISH/HINDI/TAMIL/TELUGU/KANNADA/MALAYALAM): ");
            String lang = scanner.nextLine().trim().toUpperCase();

            System.out.print("Enter Duration (minutes): ");
            String duration = scanner.nextLine().trim();

            System.out.print("Enter Rating (e.g. 8.5): ");
            String rating = scanner.nextLine().trim();

            System.out.print("Enter Synopsis/Description: ");
            String desc = scanner.nextLine().trim();

            sendCommand("ADMIN_ADD_MOVIE " + title + "|" + genre + "|" + lang + "|" + duration + "|" + rating + "|" + desc);
            readAndPrintSingleOrMultiResponse();
        }
    }

    private void showOsDiagnostics() {
        sendCommand("OS_INFO");
        readAndPrintBlockResponse();
    }

    private void sendCommand(String command) {
        out.println(command);
    }

    private void readAndPrintBlockResponse() {
        try {
            String firstLine = in.readLine();
            if (firstLine == null) return;

            if (firstLine.equals("DATA_START")) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("DATA_END")) break;
                    System.out.println(line);
                }
            } else {
                System.out.println(firstLine);
            }
        } catch (IOException e) {
            System.err.println("Error reading response from socket server: " + e.getMessage());
        }
    }

    private void readAndPrintSingleOrMultiResponse() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if (!in.ready()) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading response: " + e.getMessage());
        }
    }

    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }
}
