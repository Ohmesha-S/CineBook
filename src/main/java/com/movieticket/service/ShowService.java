package com.movieticket.service;

import com.movieticket.dto.ShowRequest;
import com.movieticket.dto.ShowResponse;
import com.movieticket.enums.MovieStatus;
import com.movieticket.enums.ShowStatus;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Movie;
import com.movieticket.model.Screen;
import com.movieticket.model.Show;
import com.movieticket.repository.MovieRepository;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;

    public ShowService(ShowRepository showRepository,
                       MovieRepository movieRepository,
                       ScreenRepository screenRepository) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getUpcomingShowsByMovie(Long movieId) {
        return showRepository.findUpcomingShowsByMovie(movieId, LocalDate.now())
                .stream()
                .map(ShowResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByTheatre(Long theatreId) {
        return showRepository.findShowsByTheatre(theatreId, LocalDate.now())
                .stream()
                .map(ShowResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovieTheatreAndDate(Long movieId, Long theatreId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return showRepository.findShowsByMovieTheatreAndDate(movieId, theatreId, queryDate)
                .stream()
                .map(ShowResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDatesByMovie(Long movieId) {
        return showRepository.findAvailableDatesByMovie(movieId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + id));
        return ShowResponse.fromEntity(show);
    }

    public Show getShowEntity(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + id));
    }

    // Admin Operations
    public List<ShowResponse> adminGetAllShows() {
        return showRepository.findAll()
                .stream()
                .map(ShowResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShowResponse adminCreateShow(ShowRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));

        if (movie.getStatus() == MovieStatus.INACTIVE) {
            throw new BadRequestException("Cannot schedule show for an inactive movie.");
        }

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + request.getScreenId()));

        // Date/Time validation
        LocalDate today = LocalDate.now();
        if (request.getShowDate().isBefore(today)) {
            throw new BadRequestException("Show date cannot be in the past.");
        }

        LocalTime startTime = request.getStartTime();
        if (request.getShowDate().isEqual(today) && startTime.isBefore(LocalTime.now())) {
            throw new BadRequestException("Show time cannot be in the past for today's schedule.");
        }

        // Calculate end time automatically from movie duration (+ 15 mins buffer) if not provided
        LocalTime endTime = request.getEndTime();
        if (endTime == null) {
            int durationWithCleaning = (movie.getDurationMinutes() != null ? movie.getDurationMinutes() : 120) + 15;
            endTime = startTime.plusMinutes(durationWithCleaning);
        }

        // Overlap validation: Two shows on the same screen must not overlap
        List<Show> conflicts = showRepository.findConflictingShows(screen.getId(), request.getShowDate(), startTime, endTime, null);
        if (!conflicts.isEmpty()) {
            Show conflict = conflicts.get(0);
            throw new BadRequestException("Schedule conflict: Screen '" + screen.getScreenName() +
                    "' is already booked for another show from " + conflict.getStartTime() + " to " + conflict.getEndTime());
        }

        Show show = new Show(
                movie,
                screen,
                request.getShowDate(),
                startTime,
                endTime,
                request.getStatus() != null ? request.getStatus() : ShowStatus.SCHEDULED
        );
        if (request.getPriceMultiplier() != null && request.getPriceMultiplier() > 0) {
            show.setPriceMultiplier(request.getPriceMultiplier());
        }

        show = showRepository.save(show);
        return ShowResponse.fromEntity(show);
    }

    @Transactional
    public ShowResponse adminUpdateShow(Long id, ShowRequest request) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + id));

        if (request.getMovieId() != null) {
            Movie movie = movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));
            show.setMovie(movie);
        }

        if (request.getScreenId() != null) {
            Screen screen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + request.getScreenId()));
            show.setScreen(screen);
        }

        if (request.getShowDate() != null) {
            show.setShowDate(request.getShowDate());
        }

        if (request.getStartTime() != null) {
            show.setStartTime(request.getStartTime());
        }

        if (request.getEndTime() != null) {
            show.setEndTime(request.getEndTime());
        }

        if (request.getStatus() != null) {
            show.setStatus(request.getStatus());
        }

        if (request.getPriceMultiplier() != null) {
            show.setPriceMultiplier(request.getPriceMultiplier());
        }

        // Validate conflicts excluding current show
        List<Show> conflicts = showRepository.findConflictingShows(
                show.getScreen().getId(),
                show.getShowDate(),
                show.getStartTime(),
                show.getEndTime(),
                show.getId()
        );
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Schedule conflict: Another show already occupies this screen during the requested time.");
        }

        show = showRepository.save(show);
        return ShowResponse.fromEntity(show);
    }

    @Transactional
    public void adminCancelShow(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + id));
        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);
    }
}
