package com.movieticket.service;

import com.movieticket.dto.MovieRequest;
import com.movieticket.dto.MovieResponse;
import com.movieticket.enums.MovieStatus;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Movie;
import com.movieticket.model.comparator.MovieTitleComparator;
import com.movieticket.repository.MovieRepository;
import com.movieticket.util.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit 2 & Unit 3: Spring Boot @Service using ArrayList, Comparable (CollectionUtils.sortNaturally),
 * Concrete Comparator (MovieTitleComparator), and classic loops.
 */
@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<MovieResponse> getAllActiveMovies() {
        List<Movie> movies = movieRepository.findByStatusNot(MovieStatus.INACTIVE);
        CollectionUtils.sortNaturally(movies);
        return toResponseList(movies);
    }

    public List<MovieResponse> getNowShowingMovies() {
        List<Movie> movies = movieRepository.findByStatus(MovieStatus.NOW_SHOWING);
        CollectionUtils.sortNaturally(movies);
        return toResponseList(movies);
    }

    public List<MovieResponse> getUpcomingMovies() {
        List<Movie> movies = movieRepository.findByStatus(MovieStatus.UPCOMING);
        CollectionUtils.sortWithComparator(movies, new MovieTitleComparator());
        return toResponseList(movies);
    }

    public MovieResponse getMovieById(Long id) {
        Movie movie = getMovieEntity(id);
        return MovieResponse.fromEntity(movie);
    }

    public Movie getMovieEntity(Long id) {
        Movie movie = movieRepository.findById(id);
        if (movie == null) {
            throw new ResourceNotFoundException("Movie not found with id: " + id);
        }
        return movie;
    }

    public List<MovieResponse> searchMovies(String title) {
        List<Movie> matched = movieRepository.findByTitleContainingIgnoreCase(title);
        List<Movie> active = new ArrayList<Movie>();
        for (int i = 0; i < matched.size(); i++) {
            Movie m = matched.get(i);
            if (!MovieStatus.INACTIVE.equals(m.getStatus())) {
                active.add(m);
            }
        }
        CollectionUtils.sortWithComparator(active, new MovieTitleComparator());
        return toResponseList(active);
    }

    public List<MovieResponse> filterMovies(String title, String genre, String language) {
        String titleParam = (title != null && !title.trim().isEmpty()) ? title.trim() : null;
        String genreParam = (genre != null && !genre.trim().isEmpty() && !genre.trim().equalsIgnoreCase("ALL"))
                ? genre.trim() : null;
        String langParam = (language != null && !language.trim().isEmpty() && !language.trim().equalsIgnoreCase("ALL"))
                ? language.trim() : null;

        List<Movie> movies = movieRepository.filterMovies(titleParam, genreParam, langParam);
        CollectionUtils.sortNaturally(movies);
        return toResponseList(movies);
    }

    public List<String> getAllGenres() {
        return movieRepository.findDistinctGenres();
    }

    public List<String> getAllLanguages() {
        return movieRepository.findDistinctLanguages();
    }

    public List<MovieResponse> adminGetAllMovies() {
        List<Movie> movies = movieRepository.findAll();
        CollectionUtils.sortNaturally(movies);
        return toResponseList(movies);
    }

    public MovieResponse adminCreateMovie(MovieRequest request) {
        validateMovieRequest(request);
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

    public MovieResponse adminUpdateMovie(Long id, MovieRequest request) {
        validateMovieRequest(request);
        Movie movie = getMovieEntity(id);

        movie.setTitle(request.getTitle().trim());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre().trim());
        movie.setLanguage(request.getLanguage().trim());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setRating(request.getRating());
        movie.setReleaseDate(request.getReleaseDate());
        if (request.getPosterUrl() != null) {
            movie.setPosterUrl(request.getPosterUrl());
        }
        if (request.getBackdropUrl() != null) {
            movie.setBackdropUrl(request.getBackdropUrl());
        }
        movie.setDirector(request.getDirector());
        movie.setCast(request.getCast());
        if (request.getStatus() != null) {
            movie.setStatus(request.getStatus());
        }

        movie = movieRepository.save(movie);
        return MovieResponse.fromEntity(movie);
    }

    public void adminDeleteMovie(Long id) {
        Movie movie = getMovieEntity(id);
        movie.setStatus(MovieStatus.INACTIVE);
        movieRepository.save(movie);
    }

    private void validateMovieRequest(MovieRequest request) {
        if (request == null) {
            throw new BadRequestException("Movie details are required.");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Movie title is required.");
        }
        if (request.getGenre() == null || request.getGenre().trim().isEmpty()) {
            throw new BadRequestException("Genre is required.");
        }
        if (request.getLanguage() == null || request.getLanguage().trim().isEmpty()) {
            throw new BadRequestException("Language is required.");
        }
        if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) {
            throw new BadRequestException("Duration in minutes must be positive.");
        }
    }

    private List<MovieResponse> toResponseList(List<Movie> movies) {
        List<MovieResponse> responses = new ArrayList<MovieResponse>();
        if (movies == null) {
            return responses;
        }
        for (int i = 0; i < movies.size(); i++) {
            responses.add(MovieResponse.fromEntity(movies.get(i)));
        }
        return responses;
    }
}
