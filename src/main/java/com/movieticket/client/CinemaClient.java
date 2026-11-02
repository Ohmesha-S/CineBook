package com.movieticket.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Unit 5: Networking using Java - Standalone Interactive Console Client connecting via TCP Socket,
 * InetAddress, BufferedReader, PrintWriter, and Scanner.
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
        System.out.println("            CINEBOOK CINEMAS - JAVA CONSOLE CLIENT                  ");
        System.out.println("             (Unit 5: Networking using Java TCP Sockets)            ");
        System.out.println("====================================================================");

        try {
            InetAddress serverAddress = InetAddress.getByName(host);
            System.out.println("Connecting to Cinema Server at " + serverAddress.getHostName()
                    + " (" + serverAddress.getHostAddress() + "):" + port + "...");

            this.socket = new Socket(serverAddress, port);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

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

            if ("1".equals(choice)) {
                browseMovies();
            } else if ("2".equals(choice)) {
                searchMovies();
            } else if ("3".equals(choice)) {
                viewShows();
            } else if ("4".equals(choice)) {
                viewSeats();
            } else if ("5".equals(choice)) {
                bookSeats();
            } else if ("6".equals(choice)) {
                viewMyBookings();
            } else if ("7".equals(choice)) {
                cancelBooking();
            } else if ("8".equals(choice)) {
                registerUser();
            } else if ("9".equals(choice)) {
                loginUser();
            } else if ("10".equals(choice)) {
                adminOperations();
            } else if ("11".equals(choice)) {
                showNetworkAndThreadDiagnostics();
            } else if ("12".equals(choice) || "exit".equalsIgnoreCase(choice) || "quit".equalsIgnoreCase(choice)) {
                sendCommand("QUIT");
                running = false;
            } else {
                System.out.println("Invalid option. Please choose between 1 and 12.");
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
        System.out.println(" [5] Book Movie Tickets & View Digital Receipt");
        System.out.println(" [6] View My Booking History");
        System.out.println(" [7] Cancel a Booking");
        System.out.println(" [8] Register New Customer");
        System.out.println(" [9] Customer / Admin Login");
        System.out.println(" [10] Admin Operations (Stats & Add Movie)");
        System.out.println(" [11] Networking & Multithreading Diagnostics (Units 5 & 6)");
        System.out.println(" [12] Exit");
        System.out.println("--------------------------------------------------------------------");
    }

    private void browseMovies() {
        sendCommand("MOVIES");
        readAndPrintBlockResponse();
    }

    private void searchMovies() {
        System.out.print("Enter movie title keyword to search: ");
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

            System.out.print("Enter Genre: ");
            String genre = scanner.nextLine().trim();

            System.out.print("Enter Language: ");
            String lang = scanner.nextLine().trim();

            System.out.print("Enter Duration (minutes): ");
            String duration = scanner.nextLine().trim();

            System.out.print("Enter Rating (e.g. PG-13): ");
            String rating = scanner.nextLine().trim();

            System.out.print("Enter Synopsis/Description: ");
            String desc = scanner.nextLine().trim();

            sendCommand("ADMIN_ADD_MOVIE " + title + "|" + genre + "|" + lang + "|" + duration + "|" + rating + "|" + desc);
            readAndPrintSingleOrMultiResponse();
        }
    }

    private void showNetworkAndThreadDiagnostics() {
        sendCommand("THREAD_INFO");
        readAndPrintBlockResponse();
        sendCommand("NET_INFO");
        readAndPrintBlockResponse();
    }

    private void sendCommand(String command) {
        out.println(command);
    }

    private void readAndPrintBlockResponse() {
        try {
            String firstLine = in.readLine();
            if (firstLine == null) {
                return;
            }
            if ("DATA_START".equals(firstLine)) {
                String line;
                while ((line = in.readLine()) != null) {
                    if ("DATA_END".equals(line)) {
                        break;
                    }
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
        } catch (IOException ignored) {
        }
    }
}
