package com.movieticket.service;

import com.movieticket.dto.ShowRequest;
import com.movieticket.dto.ShowResponse;
import com.movieticket.enums.MovieStatus;
import com.movieticket.enums.ShowStatus;
import com.movieticket.exception.BadRequestException;
import com.movieticket.model.Movie;
import com.movieticket.model.Screen;
import com.movieticket.model.Show;
import com.movieticket.model.Theatre;
import com.movieticket.repository.MovieRepository;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShowServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ScreenRepository screenRepository;

    @InjectMocks
    private ShowService showService;

    private Movie sampleMovie;
    private Screen sampleScreen;
    private Theatre sampleTheatre;
    private Show existingShow;

    @BeforeEach
    void setUp() {
        sampleMovie = new Movie("Interstellar", "Space", "Sci-Fi", "English", 169, "PG-13",
                LocalDate.now(), "p.jpg", "b.jpg", "Nolan", "McConaughey", MovieStatus.NOW_SHOWING);
        sampleMovie.setId(1L);

        sampleTheatre = new Theatre("PVR Grand", "Chennai", "Velachery", "123", "ACTIVE");
        sampleTheatre.setId(1L);

        sampleScreen = new Screen(sampleTheatre, "Screen 1", 5, 50, "IMAX 2D");
        sampleScreen.setId(1L);

        existingShow = new Show(sampleMovie, sampleScreen, LocalDate.now().plusDays(1),
                LocalTime.of(14, 0), LocalTime.of(17, 0), ShowStatus.SCHEDULED);
        existingShow.setId(1L);
    }

    @Test
    @DisplayName("Show 1: Retrieve available shows for movie")
    void testRetrieveAvailableShows() {
        when(showRepository.findUpcomingShowsByMovie(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(existingShow));

        List<ShowResponse> shows = showService.getUpcomingShowsByMovie(1L);

        assertEquals(1, shows.size());
        assertEquals("Screen 1", shows.get(0).getScreenName());
        assertEquals(LocalTime.of(14, 0), shows.get(0).getStartTime());
    }

    @Test
    @DisplayName("Show 2: Reject past show scheduling")
    void testRejectPastShowDate() {
        ShowRequest pastRequest = new ShowRequest();
        pastRequest.setMovieId(1L);
        pastRequest.setScreenId(1L);
        pastRequest.setShowDate(LocalDate.now().minusDays(2));
        pastRequest.setStartTime(LocalTime.of(10, 0));

        when(movieRepository.findById(1L)).thenReturn(Optional.of(sampleMovie));
        when(screenRepository.findById(1L)).thenReturn(Optional.of(sampleScreen));

        assertThrows(BadRequestException.class, () -> showService.adminCreateShow(pastRequest));
        verify(showRepository, never()).save(any());
    }

    @Test
    @DisplayName("Show 3: Reject overlapping screen schedules")
    void testRejectOverlappingShowSchedule() {
        ShowRequest conflictRequest = new ShowRequest();
        conflictRequest.setMovieId(1L);
        conflictRequest.setScreenId(1L);
        conflictRequest.setShowDate(LocalDate.now().plusDays(1));
        conflictRequest.setStartTime(LocalTime.of(15, 0)); // Overlaps with 14:00 - 17:00

        when(movieRepository.findById(1L)).thenReturn(Optional.of(sampleMovie));
        when(screenRepository.findById(1L)).thenReturn(Optional.of(sampleScreen));
        when(showRepository.findConflictingShows(eq(1L), eq(conflictRequest.getShowDate()), any(), any(), isNull()))
                .thenReturn(List.of(existingShow));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> showService.adminCreateShow(conflictRequest));
        assertTrue(ex.getMessage().contains("Schedule conflict"));
        verify(showRepository, never()).save(any());
    }

    @Test
    @DisplayName("Show 4: Allow non-conflicting shows on same screen")
    void testAllowNonConflictingShow() {
        ShowRequest validRequest = new ShowRequest();
        validRequest.setMovieId(1L);
        validRequest.setScreenId(1L);
        validRequest.setShowDate(LocalDate.now().plusDays(1));
        validRequest.setStartTime(LocalTime.of(18, 0)); // Starts after 17:00
        validRequest.setEndTime(LocalTime.of(21, 0));

        when(movieRepository.findById(1L)).thenReturn(Optional.of(sampleMovie));
        when(screenRepository.findById(1L)).thenReturn(Optional.of(sampleScreen));
        when(showRepository.findConflictingShows(eq(1L), eq(validRequest.getShowDate()), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());

        Show newShow = new Show(sampleMovie, sampleScreen, validRequest.getShowDate(),
                validRequest.getStartTime(), validRequest.getEndTime(), ShowStatus.SCHEDULED);
        newShow.setId(2L);
        when(showRepository.save(any(Show.class))).thenReturn(newShow);

        ShowResponse created = showService.adminCreateShow(validRequest);
        assertNotNull(created);
        assertEquals(LocalTime.of(18, 0), created.getStartTime());
    }
}
