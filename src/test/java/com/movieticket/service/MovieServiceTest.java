package com.movieticket.service;

import com.movieticket.dto.MovieResponse;
import com.movieticket.enums.MovieStatus;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Movie;
import com.movieticket.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private Movie interstellar;
    private Movie cyberpunk;

    @BeforeEach
    void setUp() {
        interstellar = new Movie("Interstellar", "Space exploration", "Sci-Fi", "English",
                Integer.valueOf(169), "PG-13", LocalDate.now(), "poster1.jpg", "back1.jpg",
                "Christopher Nolan", "Matthew M", MovieStatus.NOW_SHOWING);
        interstellar.setId(Long.valueOf(1L));

        cyberpunk = new Movie("Cyberpunk Neo", "Future world", "Action", "English",
                Integer.valueOf(142), "R", LocalDate.now().minusDays(2), "poster2.jpg", "back2.jpg",
                "Denis V", "Ryan G", MovieStatus.NOW_SHOWING);
        cyberpunk.setId(Long.valueOf(2L));
    }

    @Test
    @DisplayName("Movie 1: Retrieve all active movies sorted naturally via Comparable")
    void testRetrieveAllActiveMovies() {
        when(movieRepository.findByStatusNot(MovieStatus.INACTIVE))
                .thenReturn(new ArrayList<Movie>(Arrays.asList(cyberpunk, interstellar)));

        List<MovieResponse> result = movieService.getAllActiveMovies();

        assertEquals(2, result.size());
        assertEquals("Interstellar", result.get(0).getTitle());
        assertEquals("Cyberpunk Neo", result.get(1).getTitle());
    }

    @Test
    @DisplayName("Movie 2: Retrieve movie by ID")
    void testRetrieveMovieById() {
        when(movieRepository.findById(Long.valueOf(1L))).thenReturn(interstellar);

        MovieResponse result = movieService.getMovieById(Long.valueOf(1L));

        assertNotNull(result);
        assertEquals("Interstellar", result.getTitle());
        assertEquals("Christopher Nolan", result.getDirector());
    }

    @Test
    @DisplayName("Movie 3: Search movies by title")
    void testSearchMovies() {
        when(movieRepository.findByTitleContainingIgnoreCase("stellar"))
                .thenReturn(new ArrayList<Movie>(Arrays.asList(interstellar)));

        List<MovieResponse> result = movieService.searchMovies("stellar");

        assertEquals(1, result.size());
        assertEquals("Interstellar", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Movie 4: Filter movies by genre")
    void testFilterMoviesByGenre() {
        when(movieRepository.filterMovies(null, "Sci-Fi", null))
                .thenReturn(new ArrayList<Movie>(Arrays.asList(interstellar)));

        List<MovieResponse> result = movieService.filterMovies(null, "Sci-Fi", null);

        assertEquals(1, result.size());
        assertEquals("Sci-Fi", result.get(0).getGenre());
    }

    @Test
    @DisplayName("Movie 5: Movie not found exception")
    void testMovieNotFound() {
        when(movieRepository.findById(Long.valueOf(99L))).thenReturn(null);

        try {
            movieService.getMovieById(Long.valueOf(99L));
            fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
