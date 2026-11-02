package com.movieticket.service;

import com.movieticket.dto.AuthResponse;
import com.movieticket.dto.LoginRequest;
import com.movieticket.dto.RegisterRequest;
import com.movieticket.enums.UserRole;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.UnauthorizedException;
import com.movieticket.model.User;
import com.movieticket.repository.UserRepository;
import com.movieticket.util.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Rahul Sharma", "rahul@example.com", "9876543210", "hashed_pwd", UserRole.CUSTOMER);
        sampleUser.setId(Long.valueOf(1L));
    }

    @Test
    @DisplayName("Should successfully register a new customer")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("Rahul Sharma", "rahul@example.com", "9876543210", "Password@123");

        when(Boolean.valueOf(userRepository.existsByEmail("rahul@example.com"))).thenReturn(Boolean.FALSE);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed_pwd");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("rahul@example.com", response.getUser().getEmail());
        assertEquals(UserRole.CUSTOMER, response.getUser().getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when registering duplicate email")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Rahul Sharma", "rahul@example.com", "9876543210", "Password@123");
        when(Boolean.valueOf(userRepository.existsByEmail("rahul@example.com"))).thenReturn(Boolean.TRUE);

        try {
            authService.register(request);
            fail("Expected BadRequestException");
        } catch (BadRequestException expected) {
            assertNotNull(expected.getMessage());
        }
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("rahul@example.com", "Password@123");

        when(userRepository.findByEmail("rahul@example.com")).thenReturn(sampleUser);
        when(Boolean.valueOf(passwordEncoder.matches("Password@123", "hashed_pwd"))).thenReturn(Boolean.TRUE);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("rahul@example.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException for bad password")
    void testLoginBadPassword() {
        LoginRequest request = new LoginRequest("rahul@example.com", "WrongPassword");

        when(userRepository.findByEmail("rahul@example.com")).thenReturn(sampleUser);
        when(Boolean.valueOf(passwordEncoder.matches("WrongPassword", "hashed_pwd"))).thenReturn(Boolean.FALSE);

        try {
            authService.login(request);
            fail("Expected UnauthorizedException");
        } catch (UnauthorizedException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw UnauthorizedException for non-existent user")
    void testLoginUserNotFound() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "Password@123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        try {
            authService.login(request);
            fail("Expected UnauthorizedException");
        } catch (UnauthorizedException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
