package com.movieticket.service;

import com.movieticket.dto.MovieRequest;
import com.movieticket.dto.MovieResponse;
import com.movieticket.enums.MovieStatus;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Movie;
import com.movieticket.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<MovieResponse> getAllActiveMovies() {
        return movieRepository.findByStatusNot(MovieStatus.INACTIVE)
                .stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<MovieResponse> getNowShowingMovies() {
        return movieRepository.findByStatus(MovieStatus.NOW_SHOWING)
                .stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<MovieResponse> getUpcomingMovies() {
        return movieRepository.findByStatus(MovieStatus.UPCOMING)
                .stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        return MovieResponse.fromEntity(movie);
    }

    public Movie getMovieEntity(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
    }

    public List<MovieResponse> searchMovies(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .filter(m -> m.getStatus() != MovieStatus.INACTIVE)
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<MovieResponse> filterMovies(String title, String genre, String language) {
        String titleParam = (title != null && !title.trim().isEmpty()) ? title.trim() : null;
        String genreParam = (genre != null && !genre.trim().equalsIgnoreCase("ALL")) ? genre.trim() : null;
        String langParam = (language != null && !language.trim().equalsIgnoreCase("ALL")) ? language.trim() : null;

        return movieRepository.filterMovies(titleParam, genreParam, langParam)
                .stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<String> getAllGenres() {
        return movieRepository.findDistinctGenres();
    }

    public List<String> getAllLanguages() {
        return movieRepository.findDistinctLanguages();
    }

    // Admin Operations
    public List<MovieResponse> adminGetAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public MovieResponse adminCreateMovie(MovieRequest request) {
        Movie movie = new Movie(
                request.getTitle().trim(),
                request.getDescription(),
                request.getGenre().trim(),
                request.getLanguage().trim(),
                request.getDurationMinutes(),
                request.getRating(),
                request.getReleaseDate(),
                request.getPosterUrl(),
                request.getBackdropUrl(),
                request.getDirector(),
                request.getCast(),
                request.getStatus() != null ? request.getStatus() : MovieStatus.NOW_SHOWING
        );
        movie = movieRepository.save(movie);
        return MovieResponse.fromEntity(movie);
    }

    @Transactional
    public MovieResponse adminUpdateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));

        movie.setTitle(request.getTitle().trim());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre().trim());
        movie.setLanguage(request.getLanguage().trim());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setRating(request.getRating());
        movie.setReleaseDate(request.getReleaseDate());
        if (request.getPosterUrl() != null) movie.setPosterUrl(request.getPosterUrl());
        if (request.getBackdropUrl() != null) movie.setBackdropUrl(request.getBackdropUrl());
        movie.setDirector(request.getDirector());
        movie.setCast(request.getCast());
        if (request.getStatus() != null) movie.setStatus(request.getStatus());

        movie = movieRepository.save(movie);
        return MovieResponse.fromEntity(movie);
    }

    @Transactional
    public void adminDeleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        // Soft-deactivate to avoid breaking existing bookings
        movie.setStatus(MovieStatus.INACTIVE);
        movieRepository.save(movie);
    }
}
