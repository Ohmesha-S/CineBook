package com.movieticket.service;

import com.movieticket.dto.AuthResponse;
import com.movieticket.dto.LoginRequest;
import com.movieticket.dto.RegisterRequest;
import com.movieticket.dto.UserResponse;
import com.movieticket.enums.UserRole;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.exception.UnauthorizedException;
import com.movieticket.model.User;
import com.movieticket.repository.UserRepository;
import com.movieticket.util.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Active session store for networking and console clients (Java Collections & Concurrency)
    private final Map<String, User> activeSessions = new ConcurrentHashMap<>();
    private static final ThreadLocal<User> currentSessionUser = new ThreadLocal<>();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static void setCurrentSessionUser(User user) {
        currentSessionUser.set(user);
    }

    public static void clearCurrentSessionUser() {
        currentSessionUser.remove();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email address is already registered.");
        }

        User user = new User(
                request.getFullName().trim(),
                request.getEmail().trim().toLowerCase(),
                request.getPhone() != null ? request.getPhone().trim() : "",
                passwordEncoder.encode(request.getPassword()),
                UserRole.CUSTOMER
        );

        User saved = userRepository.save(user);
        String sessionToken = UUID.randomUUID().toString();
        activeSessions.put(sessionToken, saved);
        currentSessionUser.set(saved);

        return new AuthResponse(sessionToken, UserResponse.fromEntity(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String sessionToken = UUID.randomUUID().toString();
        activeSessions.put(sessionToken, user);
        currentSessionUser.set(user);

        return new AuthResponse(sessionToken, UserResponse.fromEntity(user));
    }

    public User getCurrentAuthenticatedUser() {
        User user = currentSessionUser.get();
        if (user != null) {
            return user;
        }

        // Fallback for console / default customer
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .findFirst()
                .orElseGet(() -> userRepository.findAll().stream()
                        .findFirst()
                        .orElseThrow(() -> new UnauthorizedException("No authenticated user session active.")));
    }

    public UserResponse getCurrentUserProfile() {
        return UserResponse.fromEntity(getCurrentAuthenticatedUser());
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User getUserBySession(String sessionToken) {
        if (sessionToken == null) return null;
        return activeSessions.get(sessionToken);
    }
}
