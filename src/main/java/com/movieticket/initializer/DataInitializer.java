package com.movieticket.initializer;

import com.movieticket.enums.MovieStatus;
import com.movieticket.enums.ShowStatus;
import com.movieticket.enums.UserRole;
import com.movieticket.model.Movie;
import com.movieticket.model.Screen;
import com.movieticket.model.Show;
import com.movieticket.model.Theatre;
import com.movieticket.model.User;
import com.movieticket.repository.MovieRepository;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.ShowRepository;
import com.movieticket.repository.TheatreRepository;
import com.movieticket.repository.UserRepository;
import com.movieticket.service.ScreenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import com.movieticket.util.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final ShowRepository showRepository;
    private final ScreenService screenService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@cinebook.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Value("${app.admin.phone:9876543210}")
    private String adminPhone;

    public DataInitializer(UserRepository userRepository,
                           MovieRepository movieRepository,
                           TheatreRepository theatreRepository,
                           ScreenRepository screenRepository,
                           ShowRepository showRepository,
                           ScreenService screenService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.screenRepository = screenRepository;
        this.showRepository = showRepository;
        this.screenService = screenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initUsers();
        initMoviesAndTheatres();
    }

    private void initUsers() {
        userRepository.findByEmail(adminEmail.toLowerCase()).ifPresentOrElse(
                admin -> {
                    if (admin.getPasswordHash() == null || !admin.getPasswordHash().contains(":")) {
                        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                        userRepository.save(admin);
                        logger.info("Updated Admin password hash: {}", adminEmail);
                    }
                },
                () -> {
                    User admin = new User(
                            adminName,
                            adminEmail.toLowerCase(),
                            adminPhone,
                            passwordEncoder.encode(adminPassword),
                            UserRole.ADMIN
                    );
                    userRepository.save(admin);
                    logger.info("Initialized Admin account: {}", adminEmail);
                }
        );

        String customerEmail = "customer@cinebook.com";
        userRepository.findByEmail(customerEmail).ifPresentOrElse(
                customer -> {
                    if (customer.getPasswordHash() == null || !customer.getPasswordHash().contains(":")) {
                        customer.setPasswordHash(passwordEncoder.encode("Customer@123"));
                        userRepository.save(customer);
                        logger.info("Updated Customer password hash: {}", customerEmail);
                    }
                },
                () -> {
                    User customer = new User(
                            "Rahul Sharma",
                            customerEmail,
                            "9876543211",
                            passwordEncoder.encode("Customer@123"),
                            UserRole.CUSTOMER
                    );
                    userRepository.save(customer);
                    logger.info("Initialized sample Customer account: {}", customerEmail);
                }
        );
    }

    private void initMoviesAndTheatres() {
        if (movieRepository.count() > 0) {
            logger.info("Database already contains movies. Skipping seed data population.");
            return;
        }

        logger.info("Seeding initial movies, theatres, screens, seat layouts, and shows...");

        // 1. Seed 8+ Realistic Movies
        List<Movie> movies = new ArrayList<>();

        movies.add(new Movie(
                "Interstellar: Beyond Time",
                "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot, Joseph Cooper, is tasked to pilot a spacecraft along with a team of researchers to find a new planet for humans.",
                "Sci-Fi / Adventure",
                "English",
                169,
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
                142,
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
                152,
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
                128,
                "PG",
                LocalDate.now().minusDays(5),
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1469488865564-c2de10f69f96?w=1600&auto=format&fit=crop&q=80",
                "Damien Chazelle",
                "Emma Stone, Timothée Chalamet, Florence Pugh",
                MovieStatus.NOW_SHOWING
        ));

        movies.add(new Movie(
                "Shadow Realm: The Awakening",
                "Paranormal investigators travel to an ancient Scottish castle following mysterious supernatural distress signals, unlocking a gateway to an unforgiving ethereal dimension.",
                "Horror / Mystery",
                "English",
                115,
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
                180,
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
                166,
                "PG-13",
                LocalDate.now().plusDays(14),
                "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1600&auto=format&fit=crop&q=80",
                "Denis Villeneuve",
                "Timothée Chalamet, Zendaya, Javier Bardem",
                MovieStatus.UPCOMING
        ));

        movies.add(new Movie(
                "Speed Demons: Velocity",
                "Underground street racers and undercover detectives unite across Tokyo, Monte Carlo, and Dubai for the ultimate heist on wheels.",
                "Action / Thriller",
                "English",
                135,
                "U/A",
                LocalDate.now().plusDays(25),
                "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=1600&auto=format&fit=crop&q=80",
                "Justin Lin",
                "Vin Diesel, Paul Walker Jr., Michelle Rodriguez",
                MovieStatus.UPCOMING
        ));

        movies = movieRepository.saveAll(movies);

        // 2. Seed 3 Theatres
        Theatre t1 = new Theatre("PVR INOX Grand Mall", "Chennai", "Velachery Main Road, Phoenix Marketcity, Chennai", "+91 44 6651 3000", "ACTIVE");
        Theatre t2 = new Theatre("Cinepolis Nexus Multiplex", "Bangalore", "Koramangala 7th Block, Hosur Road, Bangalore", "+91 80 4123 4567", "ACTIVE");
        Theatre t3 = new Theatre("PVR IMAX Platinum", "Mumbai", "High Street Phoenix, Lower Parel, Mumbai", "+91 22 2490 8000", "ACTIVE");

        theatreRepository.save(t1);
        theatreRepository.save(t2);
        theatreRepository.save(t3);

        // 3. Seed 2 Screens per theatre (6 screens total) with auto-generated tiered seats
        List<Screen> allScreens = new ArrayList<>();

        Screen s1 = new Screen(t1, "Screen 1 - IMAX Laser 3D", 5, 50, "IMAX Laser 3D");
        s1 = screenRepository.save(s1);
        screenService.generateSeatLayout(s1, 5, 10);
        allScreens.add(s1);

        Screen s2 = new Screen(t1, "Screen 2 - Dolby Atmos 4K", 5, 50, "Dolby Atmos 4K");
        s2 = screenRepository.save(s2);
        screenService.generateSeatLayout(s2, 5, 10);
        allScreens.add(s2);

        Screen s3 = new Screen(t2, "Audi 1 - 4DX Dynamic", 5, 50, "4DX Dynamic");
        s3 = screenRepository.save(s3);
        screenService.generateSeatLayout(s3, 5, 10);
        allScreens.add(s3);

        Screen s4 = new Screen(t2, "Audi 2 - Dolby Cinema", 5, 50, "Dolby Cinema");
        s4 = screenRepository.save(s4);
        screenService.generateSeatLayout(s4, 5, 10);
        allScreens.add(s4);

        Screen s5 = new Screen(t3, "Platinum Screen 1 - IMAX Grand", 5, 50, "IMAX Grand");
        s5 = screenRepository.save(s5);
        screenService.generateSeatLayout(s5, 5, 10);
        allScreens.add(s5);

        Screen s6 = new Screen(t3, "Platinum Screen 2 - Gold Class Recliner", 5, 50, "Gold Class");
        s6 = screenRepository.save(s6);
        screenService.generateSeatLayout(s6, 5, 10);
        allScreens.add(s6);

        // 4. Seed Movie Shows for today, tomorrow, and day after tomorrow
        LocalDate today = LocalDate.now();
        List<Show> shows = new ArrayList<>();

        // Create shows across multiple screens and timings
        LocalTime[] timings = new LocalTime[]{
                LocalTime.of(10, 0),
                LocalTime.of(13, 30),
                LocalTime.of(17, 0),
                LocalTime.of(20, 30)
        };

        // Movie 1 in Screen 1 & Screen 3
        Movie m1 = movies.get(0);
        for (int d = 0; d < 3; d++) {
            LocalDate showDate = today.plusDays(d);
            for (int t = 0; t < timings.length; t++) {
                LocalTime start = timings[t];
                LocalTime end = start.plusMinutes(169 + 15);
                shows.add(new Show(m1, s1, showDate, start, end, ShowStatus.SCHEDULED));
            }
        }

        // Movie 2 in Screen 2 & Screen 5
        Movie m2 = movies.get(1);
        for (int d = 0; d < 3; d++) {
            LocalDate showDate = today.plusDays(d);
            for (int t = 0; t < timings.length; t++) {
                LocalTime start = timings[t];
                LocalTime end = start.plusMinutes(142 + 15);
                shows.add(new Show(m2, s2, showDate, start, end, ShowStatus.SCHEDULED));
            }
        }

        // Movie 3 in Screen 3 & Screen 6
        Movie m3 = movies.get(2);
        for (int d = 0; d < 3; d++) {
            LocalDate showDate = today.plusDays(d);
            for (int t = 0; t < timings.length; t++) {
                LocalTime start = timings[t];
                LocalTime end = start.plusMinutes(152 + 15);
                shows.add(new Show(m3, s3, showDate, start, end, ShowStatus.SCHEDULED));
            }
        }

        // Movie 6 in Screen 4
        Movie m6 = movies.get(5);
        for (int d = 0; d < 3; d++) {
            LocalDate showDate = today.plusDays(d);
            for (int t = 0; t < timings.length; t++) {
                LocalTime start = timings[t];
                LocalTime end = start.plusMinutes(180 + 15);
                shows.add(new Show(m6, s4, showDate, start, end, ShowStatus.SCHEDULED));
            }
        }

        showRepository.saveAll(shows);
        logger.info("Database initialized successfully with {} movies, {} theatres, {} screens, and {} shows!",
                movies.size(), 3, allScreens.size(), shows.size());
    }
}
