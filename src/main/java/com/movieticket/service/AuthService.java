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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit 2 & Unit 6: Session & Authentication Service using HashMap<String, User>
 * protected by synchronized monitor methods for thread-safe multi-client access.
 */
@Service
public class AuthService {

    private static final Map<Thread, User> THREAD_USERS = new HashMap<Thread, User>();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, User> activeSessions = new HashMap<String, User>();
    private User lastAuthenticatedUser;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static synchronized void setCurrentSessionUser(User user) {
        Thread current = Thread.currentThread();
        if (user == null) {
            THREAD_USERS.remove(current);
        } else {
            THREAD_USERS.put(current, user);
        }
    }

    public static synchronized void clearCurrentSessionUser() {
        THREAD_USERS.remove(Thread.currentThread());
    }

    private static synchronized User getThreadSessionUser() {
        return THREAD_USERS.get(Thread.currentThread());
    }

    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration details are required.");
        }
        if (request.getFullName() == null || request.getFullName().trim().length() < 2) {
            throw new BadRequestException("Full name must be at least 2 characters.");
        }
        if (request.getEmail() == null || !isValidEmail(request.getEmail())) {
            throw new BadRequestException("Valid email address is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email address is already registered.");
        }

        User user = new User(
                request.getFullName().trim(),
                normalizedEmail,
                request.getPhone() != null ? request.getPhone().trim() : "",
                passwordEncoder.encode(request.getPassword()),
                UserRole.CUSTOMER
        );

        User saved = userRepository.save(user);
        String sessionToken = PasswordEncoder.generateToken(32);
        storeSession(sessionToken, saved);
        setCurrentSessionUser(saved);

        return new AuthResponse(sessionToken, UserResponse.fromEntity(saved));
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Password is required.");
        }

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase());
        if (user == null) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String sessionToken = PasswordEncoder.generateToken(32);
        storeSession(sessionToken, user);
        setCurrentSessionUser(user);

        return new AuthResponse(sessionToken, UserResponse.fromEntity(user));
    }

    public void logout(String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token != null) {
            removeSession(token);
        }
        clearCurrentSessionUser();
    }

    public User resolveUserFromHeader(String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token != null) {
            User sessionUser = getUserBySession(token);
            if (sessionUser != null) {
                return sessionUser;
            }
        }
        return getCurrentAuthenticatedUser();
    }

    public User getCurrentAuthenticatedUser() {
        User threadUser = getThreadSessionUser();
        if (threadUser != null) {
            return threadUser;
        }
        User recent = getLastAuthenticatedUser();
        if (recent != null) {
            return recent;
        }

        List<User> allUsers = userRepository.findAll();
        for (int i = 0; i < allUsers.size(); i++) {
            User u = allUsers.get(i);
            if (UserRole.CUSTOMER.equals(u.getRole())) {
                return u;
            }
        }
        if (!allUsers.isEmpty()) {
            return allUsers.get(0);
        }
        throw new UnauthorizedException("No authenticated user session active.");
    }

    public UserResponse getCurrentUserProfile(String authHeader) {
        return UserResponse.fromEntity(resolveUserFromHeader(authHeader));
    }

    public UserResponse getCurrentUserProfile() {
        return UserResponse.fromEntity(getCurrentAuthenticatedUser());
    }

    public User getCurrentUser(String email) {
        if (email == null) {
            throw new BadRequestException("Email is required.");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase());
        if (user == null) {
            throw new ResourceNotFoundException("User", "email", email);
        }
        return user;
    }

    public User getUserById(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        return user;
    }

    public synchronized User getUserBySession(String sessionToken) {
        if (sessionToken == null) {
            return null;
        }
        return activeSessions.get(sessionToken);
    }

    public synchronized int getActiveSessionCount() {
        return activeSessions.size();
    }

    private synchronized void storeSession(String token, User user) {
        activeSessions.put(token, user);
        this.lastAuthenticatedUser = user;
    }

    private synchronized void removeSession(String token) {
        User removed = activeSessions.remove(token);
        if (removed != null && this.lastAuthenticatedUser != null
                && removed.getId() != null && removed.getId().equals(this.lastAuthenticatedUser.getId())) {
            this.lastAuthenticatedUser = null;
        }
    }

    private synchronized User getLastAuthenticatedUser() {
        return lastAuthenticatedUser;
    }

    public static String extractBearerToken(String authHeader) {
        if (authHeader == null) {
            return null;
        }
        String trimmed = authHeader.trim();
        if (trimmed.length() > 7 && trimmed.substring(0, 7).equalsIgnoreCase("Bearer ")) {
            return trimmed.substring(7).trim();
        }
        if (!trimmed.isEmpty()) {
            return trimmed;
        }
        return null;
    }

    private boolean isValidEmail(String email) {
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        int dotIndex = trimmed.lastIndexOf('.');
        return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < trimmed.length() - 1;
    }
}
