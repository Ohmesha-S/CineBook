package com.movieticket.service;

import com.movieticket.dto.BookingResponse;
import com.movieticket.dto.DashboardStatsDto;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.UserRole;
import com.movieticket.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public DashboardService(MovieRepository movieRepository,
                            TheatreRepository theatreRepository,
                            ScreenRepository screenRepository,
                            ShowRepository showRepository,
                            UserRepository userRepository,
                            BookingRepository bookingRepository) {
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.screenRepository = screenRepository;
        this.showRepository = showRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setTotalMovies(movieRepository.count());
        stats.setTotalTheatres(theatreRepository.count());
        stats.setTotalScreens(screenRepository.count());
        stats.setTotalShows(showRepository.count());
        stats.setTotalCustomers(userRepository.countByRole(UserRole.CUSTOMER));
        stats.setTotalBookings(bookingRepository.count());
        stats.setConfirmedBookings(bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED));
        stats.setCancelledBookings(bookingRepository.countByBookingStatus(BookingStatus.CANCELLED));

        Double revenue = bookingRepository.sumRevenueByStatus(BookingStatus.CONFIRMED);
        stats.setTotalRevenue(revenue != null ? Math.round(revenue * 100.0) / 100.0 : 0.0);

        List<BookingResponse> recent = bookingRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .limit(10)
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
        stats.setRecentBookings(recent);

        return stats;
    }
}
