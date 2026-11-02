package com.movieticket.service;

import com.movieticket.dto.BookingResponse;
import com.movieticket.dto.DashboardStatsDto;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.UserRole;
import com.movieticket.model.Booking;
import com.movieticket.model.comparator.BookingTimeDescComparator;
import com.movieticket.repository.BookingRepository;
import com.movieticket.repository.MovieRepository;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.ShowRepository;
import com.movieticket.repository.TheatreRepository;
import com.movieticket.repository.UserRepository;
import com.movieticket.util.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

        List<Booking> allBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        CollectionUtils.sortWithComparator(allBookings, new BookingTimeDescComparator());
        List<Booking> topTen = CollectionUtils.takeFirst(allBookings, 10);

        List<BookingResponse> recent = new ArrayList<BookingResponse>();
        for (int i = 0; i < topTen.size(); i++) {
            recent.add(BookingResponse.fromEntity(topTen.get(i)));
        }
        stats.setRecentBookings(recent);

        return stats;
    }
}
