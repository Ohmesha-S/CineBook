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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private Show existingShow;

    @BeforeEach
    void setUp() {
        sampleMovie = new Movie("Interstellar", "Space", "Sci-Fi", "English", Integer.valueOf(169), "PG-13",
                LocalDate.now(), "p.jpg", "b.jpg", "Nolan", "McConaughey", MovieStatus.NOW_SHOWING);
        sampleMovie.setId(Long.valueOf(1L));

        Theatre sampleTheatre = new Theatre("PVR Grand", "Chennai", "Velachery", "123", "ACTIVE");
        sampleTheatre.setId(Long.valueOf(1L));

        sampleScreen = new Screen(sampleTheatre, "Screen 1", Integer.valueOf(5), Integer.valueOf(50), "IMAX 2D");
        sampleScreen.setId(Long.valueOf(1L));

        existingShow = new Show(sampleMovie, sampleScreen, LocalDate.now().plusDays(1),
                LocalTime.of(14, 0), LocalTime.of(17, 0), ShowStatus.SCHEDULED);
        existingShow.setId(Long.valueOf(1L));
    }

    @Test
    @DisplayName("Show 1: Retrieve available shows for movie")
    void testRetrieveAvailableShows() {
        when(showRepository.findUpcomingShowsByMovie(eq(Long.valueOf(1L)), any(LocalDate.class)))
                .thenReturn(new ArrayList<Show>(Arrays.asList(existingShow)));

        List<ShowResponse> shows = showService.getUpcomingShowsByMovie(Long.valueOf(1L));

        assertEquals(1, shows.size());
        assertEquals("Screen 1", shows.get(0).getScreenName());
        assertEquals(LocalTime.of(14, 0), shows.get(0).getStartTime());
    }

    @Test
    @DisplayName("Show 2: Reject past show scheduling")
    void testRejectPastShowDate() {
        ShowRequest pastRequest = new ShowRequest();
        pastRequest.setMovieId(Long.valueOf(1L));
        pastRequest.setScreenId(Long.valueOf(1L));
        pastRequest.setShowDate(LocalDate.now().minusDays(2));
        pastRequest.setStartTime(LocalTime.of(10, 0));

        when(movieRepository.findById(Long.valueOf(1L))).thenReturn(sampleMovie);
        when(screenRepository.findById(Long.valueOf(1L))).thenReturn(sampleScreen);

        try {
            showService.adminCreateShow(pastRequest);
            fail("Expected BadRequestException");
        } catch (BadRequestException expected) {
            assertNotNull(expected.getMessage());
        }
        verify(showRepository, never()).save(any(Show.class));
    }

    @Test
    @DisplayName("Show 3: Reject overlapping screen schedules")
    void testRejectOverlappingShowSchedule() {
        ShowRequest conflictRequest = new ShowRequest();
        conflictRequest.setMovieId(Long.valueOf(1L));
        conflictRequest.setScreenId(Long.valueOf(1L));
        conflictRequest.setShowDate(LocalDate.now().plusDays(1));
        conflictRequest.setStartTime(LocalTime.of(15, 0));

        when(movieRepository.findById(Long.valueOf(1L))).thenReturn(sampleMovie);
        when(screenRepository.findById(Long.valueOf(1L))).thenReturn(sampleScreen);
        when(showRepository.findConflictingShows(
                eq(Long.valueOf(1L)), eq(conflictRequest.getShowDate()), any(LocalTime.class), any(LocalTime.class), (Long) isNull()))
                .thenReturn(Arrays.asList(existingShow));

        try {
            showService.adminCreateShow(conflictRequest);
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            assertTrue(ex.getMessage().contains("Schedule conflict"));
        }
        verify(showRepository, never()).save(any(Show.class));
    }

    @Test
    @DisplayName("Show 4: Allow non-conflicting shows on same screen")
    void testAllowNonConflictingShow() {
        ShowRequest validRequest = new ShowRequest();
        validRequest.setMovieId(Long.valueOf(1L));
        validRequest.setScreenId(Long.valueOf(1L));
        validRequest.setShowDate(LocalDate.now().plusDays(1));
        validRequest.setStartTime(LocalTime.of(18, 0));
        validRequest.setEndTime(LocalTime.of(21, 0));

        when(movieRepository.findById(Long.valueOf(1L))).thenReturn(sampleMovie);
        when(screenRepository.findById(Long.valueOf(1L))).thenReturn(sampleScreen);
        when(showRepository.findConflictingShows(
                eq(Long.valueOf(1L)), eq(validRequest.getShowDate()), any(LocalTime.class), any(LocalTime.class), (Long) isNull()))
                .thenReturn(Collections.<Show>emptyList());

        Show newShow = new Show(sampleMovie, sampleScreen, validRequest.getShowDate(),
                validRequest.getStartTime(), validRequest.getEndTime(), ShowStatus.SCHEDULED);
        newShow.setId(Long.valueOf(2L));
        when(showRepository.save(any(Show.class))).thenReturn(newShow);

        ShowResponse created = showService.adminCreateShow(validRequest);
        assertNotNull(created);
        assertEquals(LocalTime.of(18, 0), created.getStartTime());
    }
}
