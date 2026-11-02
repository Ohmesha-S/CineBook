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
import com.movieticket.util.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    public List<ShowResponse> getUpcomingShowsByMovie(Long movieId) {
        List<Show> shows = showRepository.findUpcomingShowsByMovie(movieId, LocalDate.now());
        CollectionUtils.sortNaturally(shows);
        return toResponseList(shows);
    }

    public List<ShowResponse> getShowsByTheatre(Long theatreId) {
        List<Show> shows = showRepository.findShowsByTheatre(theatreId, LocalDate.now());
        CollectionUtils.sortNaturally(shows);
        return toResponseList(shows);
    }

    public List<ShowResponse> getShowsByMovieTheatreAndDate(Long movieId, Long theatreId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<Show> shows = showRepository.findShowsByMovieTheatreAndDate(movieId, theatreId, queryDate);
        CollectionUtils.sortNaturally(shows);
        return toResponseList(shows);
    }

    public List<LocalDate> getAvailableDatesByMovie(Long movieId) {
        return showRepository.findAvailableDatesByMovie(movieId, LocalDate.now());
    }

    public ShowResponse getShowById(Long id) {
        return ShowResponse.fromEntity(getShowEntity(id));
    }

    public Show getShowEntity(Long id) {
        Show show = showRepository.findById(id);
        if (show == null) {
            throw new ResourceNotFoundException("Show not found with id: " + id);
        }
        return show;
    }

    public List<ShowResponse> adminGetAllShows() {
        List<Show> shows = showRepository.findAll();
        CollectionUtils.sortNaturally(shows);
        return toResponseList(shows);
    }

    public ShowResponse adminCreateShow(ShowRequest request) {
        if (request == null || request.getMovieId() == null || request.getScreenId() == null
                || request.getShowDate() == null || request.getStartTime() == null) {
            throw new BadRequestException("Movie, screen, show date, and start time are required.");
        }

        Movie movie = movieRepository.findById(request.getMovieId());
        if (movie == null) {
            throw new ResourceNotFoundException("Movie not found with id: " + request.getMovieId());
        }

        if (MovieStatus.INACTIVE.equals(movie.getStatus())) {
            throw new BadRequestException("Cannot schedule show for an inactive movie.");
        }

        Screen screen = screenRepository.findById(request.getScreenId());
        if (screen == null) {
            throw new ResourceNotFoundException("Screen not found with id: " + request.getScreenId());
        }

        LocalDate today = LocalDate.now();
        if (request.getShowDate().isBefore(today)) {
            throw new BadRequestException("Show date cannot be in the past.");
        }

        LocalTime startTime = request.getStartTime();
        if (request.getShowDate().isEqual(today) && startTime.isBefore(LocalTime.now())) {
            throw new BadRequestException("Show time cannot be in the past for today's schedule.");
        }

        LocalTime endTime = request.getEndTime();
        if (endTime == null) {
            int durationWithCleaning = (movie.getDurationMinutes() != null ? movie.getDurationMinutes() : 120) + 15;
            endTime = startTime.plusMinutes(durationWithCleaning);
        }

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

    public ShowResponse adminUpdateShow(Long id, ShowRequest request) {
        Show show = getShowEntity(id);

        if (request.getMovieId() != null) {
            Movie movie = movieRepository.findById(request.getMovieId());
            if (movie == null) {
                throw new ResourceNotFoundException("Movie not found with id: " + request.getMovieId());
            }
            show.setMovie(movie);
        }

        if (request.getScreenId() != null) {
            Screen screen = screenRepository.findById(request.getScreenId());
            if (screen == null) {
                throw new ResourceNotFoundException("Screen not found with id: " + request.getScreenId());
            }
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

    public void adminCancelShow(Long id) {
        Show show = getShowEntity(id);
        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);
    }

    private List<ShowResponse> toResponseList(List<Show> shows) {
        List<ShowResponse> list = new ArrayList<ShowResponse>();
        if (shows == null) {
            return list;
        }
        for (int i = 0; i < shows.size(); i++) {
            list.add(ShowResponse.fromEntity(shows.get(i)));
        }
        return list;
    }
}
