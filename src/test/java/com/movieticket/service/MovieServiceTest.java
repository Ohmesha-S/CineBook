package com.movieticket.service;

import com.movieticket.dto.MovieRequest;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
                169, "PG-13", LocalDate.now(), "poster1.jpg", "back1.jpg", "Christopher Nolan", "Matthew M", MovieStatus.NOW_SHOWING);
        interstellar.setId(1L);

        cyberpunk = new Movie("Cyberpunk Neo", "Future world", "Action", "English",
                142, "R", LocalDate.now(), "poster2.jpg", "back2.jpg", "Denis V", "Ryan G", MovieStatus.NOW_SHOWING);
        cyberpunk.setId(2L);
    }

    @Test
    @DisplayName("Movie 1: Retrieve all active movies")
    void testRetrieveAllActiveMovies() {
        when(movieRepository.findByStatusNot(MovieStatus.INACTIVE)).thenReturn(Arrays.asList(interstellar, cyberpunk));

        List<MovieResponse> result = movieService.getAllActiveMovies();

        assertEquals(2, result.size());
        assertEquals("Interstellar", result.get(0).getTitle());
        assertEquals("Cyberpunk Neo", result.get(1).getTitle());
    }

    @Test
    @DisplayName("Movie 2: Retrieve movie by ID")
    void testRetrieveMovieById() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(interstellar));

        MovieResponse result = movieService.getMovieById(1L);

        assertNotNull(result);
        assertEquals("Interstellar", result.getTitle());
        assertEquals("Christopher Nolan", result.getDirector());
    }

    @Test
    @DisplayName("Movie 3: Search movies by title")
    void testSearchMovies() {
        when(movieRepository.findByTitleContainingIgnoreCase("stellar")).thenReturn(List.of(interstellar));

        List<MovieResponse> result = movieService.searchMovies("stellar");

        assertEquals(1, result.size());
        assertEquals("Interstellar", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Movie 4: Filter movies by genre")
    void testFilterMoviesByGenre() {
        when(movieRepository.filterMovies(null, "Sci-Fi", null)).thenReturn(List.of(interstellar));

        List<MovieResponse> result = movieService.filterMovies(null, "Sci-Fi", null);

        assertEquals(1, result.size());
        assertEquals("Sci-Fi", result.get(0).getGenre());
    }

    @Test
    @DisplayName("Movie 5: Movie not found exception")
    void testMovieNotFound() {
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> movieService.getMovieById(99L));
    }
}
