package com.movieticket.initializer;

import com.movieticket.db.DatabaseManager;
import com.movieticket.enums.MovieStatus;
import com.movieticket.enums.ShowStatus;
import com.movieticket.enums.UserRole;
import com.movieticket.model.Movie;
import com.movieticket.model.Screen;
import com.movieticket.model.Show;
import com.movieticket.model.Theatre;
import com.movieticket.model.User;
import com.movieticket.os.TicketNotificationService;
import com.movieticket.repository.MovieRepository;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.ShowRepository;
import com.movieticket.repository.TheatreRepository;
import com.movieticket.repository.UserRepository;
import com.movieticket.service.ScreenService;
import com.movieticket.util.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 3, Unit 4 & Unit 6: Spring Boot Startup Initializer creating the JDBC MySQL schema,
 * seeding initial relational records, and verifying catalog readiness using Thread + join().
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final DatabaseManager databaseManager;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final ShowRepository showRepository;
    private final ScreenService screenService;
    private final PasswordEncoder passwordEncoder;
    private final TicketNotificationService notificationService;

    @Value("${app.admin.email:admin@cinebook.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Value("${app.admin.phone:9876543210}")
    private String adminPhone;

    public DataInitializer(DatabaseManager databaseManager,
                           UserRepository userRepository,
                           MovieRepository movieRepository,
                           TheatreRepository theatreRepository,
                           ScreenRepository screenRepository,
                           ShowRepository showRepository,
                           ScreenService screenService,
                           PasswordEncoder passwordEncoder,
                           TicketNotificationService notificationService) {
        this.databaseManager = databaseManager;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.screenRepository = screenRepository;
        this.showRepository = showRepository;
        this.screenService = screenService;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Override
    public void run(String... args) {
        databaseManager.initializeSchema();
        initUsers();
        initMoviesAndTheatres();
        ensureUpcomingShowsExist();
        runStartupVerificationThread();
    }

    private void initUsers() {
        String normalizedAdminEmail = adminEmail.toLowerCase();
        User existingAdmin = userRepository.findByEmail(normalizedAdminEmail);
        if (existingAdmin != null) {
            if (!passwordEncoder.matches(adminPassword, existingAdmin.getPasswordHash())) {
                existingAdmin.setPasswordHash(passwordEncoder.encode(adminPassword));
                userRepository.save(existingAdmin);
                System.out.println("[INIT] Refreshed Admin password hash for: " + normalizedAdminEmail);
            }
        } else {
            User admin = new User(
                    adminName,
                    normalizedAdminEmail,
                    adminPhone,
                    passwordEncoder.encode(adminPassword),
                    UserRole.ADMIN
            );
            userRepository.save(admin);
            System.out.println("[INIT] Initialized Admin account: " + normalizedAdminEmail);
        }

        String customerEmail = "customer@cinebook.com";
        User existingCustomer = userRepository.findByEmail(customerEmail);
        if (existingCustomer != null) {
            if (!passwordEncoder.matches("Customer@123", existingCustomer.getPasswordHash())) {
                existingCustomer.setPasswordHash(passwordEncoder.encode("Customer@123"));
                userRepository.save(existingCustomer);
                System.out.println("[INIT] Refreshed Customer password hash for: " + customerEmail);
            }
        } else {
            User customer = new User(
                    "Rahul Sharma",
                    customerEmail,
                    "9876543211",
                    passwordEncoder.encode("Customer@123"),
                    UserRole.CUSTOMER
            );
            userRepository.save(customer);
            System.out.println("[INIT] Initialized sample Customer account: " + customerEmail);
        }
    }

    private void initMoviesAndTheatres() {
        if (movieRepository.count() > 0L) {
            System.out.println("[INIT] Database already contains movies. Skipping initial catalog seed.");
            return;
        }

        System.out.println("[INIT] Seeding initial movies, theatres, screens, seat layouts, and shows via JDBC...");

        List<Movie> movies = new ArrayList<Movie>();

        movies.add(new Movie(
                "Interstellar: Beyond Time",
                "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot, Joseph Cooper, is tasked to pilot a spacecraft along with a team of researchers to find a new planet for humans.",
                "Sci-Fi / Adventure",
                "English",
                Integer.valueOf(169),
                "PG-13",
                LocalDate.now().minusDays(15),
                "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1600&auto=format&fit=crop&q=80",
                "Christopher Nolan",
                "Matthew McConaughey, Anne Hathaway, Jessica Chastain",
                MovieStatus.NOW_SHOWING
        ));

        movies.add(new Movie(
                "Cyber Horizon: Neo Tokyo",
                "In a neon-drenched metropolis of the year 2099, a rogue synthetic agent unravels a high-level cyber conspiracy that threatens the boundary between human consciousness and artificial intelligence.",
                "Action / Cyberpunk",
                "English",
                Integer.valueOf(142),
                "R",
                LocalDate.now().minusDays(8),
                "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&auto=format&fit=crop&q=80",
                "Denis Villeneuve",
                "Ryan Gosling, Ana de Armas, Harrison Ford",
                MovieStatus.NOW_SHOWING
        ));

        movies.add(new Movie(
                "The Dark Knight Legacy",
                "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.",
                "Action / Crime / Drama",
                "English",
                Integer.valueOf(152),
                "PG-13",
                LocalDate.now().minusDays(20),
                "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=1600&auto=format&fit=crop&q=80",
                "Christopher Nolan",
                "Christian Bale, Heath Ledger, Aaron Eckhart",
                MovieStatus.NOW_SHOWING
        ));

        movies.add(new Movie(
                "Echoes of Eternity",
                "A soul-stirring musical drama following two struggling artists in modern-day Vienna who discover love, ambition, and heartbreak through symphony and cinema.",
                "Drama / Romance",
                "French / English",
                Integer.valueOf(128),
                "PG",
                LocalDate.now().minusDays(5),
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1469488865564-c2de10f69f96?w=1600&auto=format&fit=crop&q=80",
                "Damien Chazelle",
                "Emma Stone, Timothee Chalamet, Florence Pugh",
                MovieStatus.NOW_SHOWING
        ));

        movies.add(new Movie(
                "Shadow Realm: The Awakening",
                "Paranormal investigators travel to an ancient Scottish castle following mysterious supernatural distress signals, unlocking a gateway to an unforgiving ethereal dimension.",
                "Horror / Mystery",
                "English",
                Integer.valueOf(115),
                "A",
                LocalDate.now().minusDays(3),
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=1600&auto=format&fit=crop&q=80",
                "James Wan",
                "Patrick Wilson, Vera Farmiga, Mia Goth",
                MovieStatus.NOW_SHOWING
        ));

        movies.add(new Movie(
                "Kalki 2898 AD: The Epic",
                "Set in a dystopian post-apocalyptic world in the city of Kasi, an immortal warrior guards the bearer of the final avatar destined to restore balance to mankind.",
                "Mythology / Sci-Fi",
                "Hindi / Telugu",
                Integer.valueOf(180),
                "U/A",
                LocalDate.now().minusDays(12),
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1600&auto=format&fit=crop&q=80",
                "Nag Ashwin",
                "Prabhas, Amitabh Bachchan, Deepika Padukone, Kamal Haasan",
                MovieStatus.NOW_SHOWING
        ));

        movies.add(new Movie(
                "Galactic Odyssey: Part II",
                "Paul and the desert tribes wage an intergalactic battle across dunes and starships to liberate their sacred planet from tyrannical galactic emperors.",
                "Sci-Fi / Epic",
                "English",
                Integer.valueOf(166),
                "PG-13",
                LocalDate.now().plusDays(14),
                "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1600&auto=format&fit=crop&q=80",
                "Denis Villeneuve",
                "Timothee Chalamet, Zendaya, Javier Bardem",
                MovieStatus.UPCOMING
        ));

        movies.add(new Movie(
                "Speed Demons: Velocity",
                "Underground street racers and undercover detectives unite across Tokyo, Monte Carlo, and Dubai for the ultimate heist on wheels.",
                "Action / Thriller",
                "English",
                Integer.valueOf(135),
                "U/A",
                LocalDate.now().plusDays(25),
                "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=1600&auto=format&fit=crop&q=80",
                "Justin Lin",
                "Vin Diesel, Paul Walker Jr., Michelle Rodriguez",
                MovieStatus.UPCOMING
        ));

        for (int i = 0; i < movies.size(); i++) {
            movieRepository.save(movies.get(i));
        }

        Theatre t1 = theatreRepository.save(new Theatre(
                "PVR INOX Grand Mall", "Chennai", "Velachery Main Road, Phoenix Marketcity, Chennai", "+91 44 6651 3000", "ACTIVE"));
        Theatre t2 = theatreRepository.save(new Theatre(
                "Cinepolis Nexus Multiplex", "Bangalore", "Koramangala 7th Block, Hosur Road, Bangalore", "+91 80 4123 4567", "ACTIVE"));
        Theatre t3 = theatreRepository.save(new Theatre(
                "PVR IMAX Platinum", "Mumbai", "High Street Phoenix, Lower Parel, Mumbai", "+91 22 2490 8000", "ACTIVE"));

        Screen s1 = screenRepository.save(new Screen(t1, "Screen 1 - IMAX Laser 3D", Integer.valueOf(5), Integer.valueOf(50), "IMAX Laser 3D"));
        screenService.generateSeatLayout(s1, 5, 10);

        Screen s2 = screenRepository.save(new Screen(t1, "Screen 2 - Dolby Atmos 4K", Integer.valueOf(5), Integer.valueOf(50), "Dolby Atmos 4K"));
        screenService.generateSeatLayout(s2, 5, 10);

        Screen s3 = screenRepository.save(new Screen(t2, "Audi 1 - 4DX Dynamic", Integer.valueOf(5), Integer.valueOf(50), "4DX Dynamic"));
        screenService.generateSeatLayout(s3, 5, 10);

        Screen s4 = screenRepository.save(new Screen(t2, "Audi 2 - Dolby Cinema", Integer.valueOf(5), Integer.valueOf(50), "Dolby Cinema"));
        screenService.generateSeatLayout(s4, 5, 10);

        Screen s5 = screenRepository.save(new Screen(t3, "Platinum Screen 1 - IMAX Grand", Integer.valueOf(5), Integer.valueOf(50), "IMAX Grand"));
        screenService.generateSeatLayout(s5, 5, 10);

        Screen s6 = screenRepository.save(new Screen(t3, "Platinum Screen 2 - Gold Class Recliner", Integer.valueOf(5), Integer.valueOf(50), "Gold Class"));
        screenService.generateSeatLayout(s6, 5, 10);

        List<Screen> allScreens = new ArrayList<Screen>();
        allScreens.add(s1);
        allScreens.add(s2);
        allScreens.add(s3);
        allScreens.add(s4);
        allScreens.add(s5);
        allScreens.add(s6);

        List<Movie> nowShowingMovies = new ArrayList<Movie>();
        for (int i = 0; i < movies.size(); i++) {
            Movie m = movies.get(i);
            if (MovieStatus.NOW_SHOWING.equals(m.getStatus())) {
                nowShowingMovies.add(m);
            }
        }
        seedShowsForNextThreeDays(nowShowingMovies, allScreens);
    }

    private void ensureUpcomingShowsExist() {
        List<Movie> nowShowing = movieRepository.findByStatus(MovieStatus.NOW_SHOWING);
        if (nowShowing.isEmpty()) {
            return;
        }
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        boolean allMoviesHaveUpcomingShows = true;
        for (int i = 0; i < nowShowing.size(); i++) {
            List<Show> futureShows = showRepository.findUpcomingShowsByMovie(nowShowing.get(i).getId(), tomorrow);
            if (futureShows.isEmpty()) {
                allMoviesHaveUpcomingShows = false;
                break;
            }
        }
        if (allMoviesHaveUpcomingShows) {
            return;
        }
        List<Screen> screens = screenRepository.findAll();
        if (screens.isEmpty()) {
            return;
        }
        System.out.println("[INIT] Refreshing upcoming show schedule across all screens and NOW_SHOWING movies...");
        seedShowsForNextThreeDays(nowShowing, screens);
    }

    private void seedShowsForNextThreeDays(List<Movie> nowShowingMovies, List<Screen> screens) {
        if (nowShowingMovies.isEmpty() || screens.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalTime[] timings = new LocalTime[] {
                LocalTime.of(10, 0),
                LocalTime.of(13, 30),
                LocalTime.of(17, 0),
                LocalTime.of(20, 30)
        };

        int pairCount = Math.max(nowShowingMovies.size(), screens.size());
        int count = 0;
        for (int d = 0; d < 3; d++) {
            LocalDate showDate = today.plusDays(d);
            for (int t = 0; t < timings.length; t++) {
                LocalTime start = timings[t];
                for (int p = 0; p < pairCount; p++) {
                    Movie movie = nowShowingMovies.get(p % nowShowingMovies.size());
                    Screen screen = screens.get(p % screens.size());
                    int duration = movie.getDurationMinutes() != null ? movie.getDurationMinutes().intValue() : 150;
                    LocalTime end = start.plusMinutes(duration + 15);
                    if (showRepository.findConflictingShows(screen.getId(), showDate, start, end, null).isEmpty()) {
                        showRepository.save(new Show(movie, screen, showDate, start, end, ShowStatus.SCHEDULED));
                        count++;
                    }
                }
            }
        }
        System.out.println("[INIT] Scheduled " + count + " upcoming shows across 3 days.");
    }

    /**
     * Unit 6: Demonstrates Thread creation with Runnable and Thread.join() synchronization on startup.
     */
    private void runStartupVerificationThread() {
        Runnable verificationTask = new Runnable() {
            @Override
            public void run() {
                long movieCount = movieRepository.count();
                long showCount = showRepository.count();
                notificationService.publishNotification(
                        "SYSTEM_READY movies=" + movieCount + " shows=" + showCount + " jdbc=CONNECTED");
            }
        };

        Thread verifierThread = new Thread(verificationTask, "CineBook-StartupVerifier");
        verifierThread.start();
        try {
            verifierThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
