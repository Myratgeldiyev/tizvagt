package com.tizvagt.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tizvagt.api.auth.dto.AuthResponse;
import com.tizvagt.api.auth.dto.LoginRequest;
import com.tizvagt.api.auth.dto.RegisterRequest;
import com.tizvagt.api.security.JwtService;
import com.tizvagt.api.user.Role;
import com.tizvagt.api.user.User;
import com.tizvagt.api.user.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
        when(jwtService.generateToken(any(), any())).thenReturn("token");
        when(jwtService.getExpiration()).thenReturn(Duration.ofHours(24));
    }

    @Test
    void registerHashesPasswordAndNormalizesEmail() {
        when(userRepository.existsByEmail("ali@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");

        AuthResponse response = authService.register(new RegisterRequest("  Ali@Example.com ", "secret123", "Ali Veli"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ali@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.expiresIn()).isEqualTo(86_400);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ali@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("ali@example.com", "secret123", "Ali")))
                .isInstanceOf(EmailAlreadyUsedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User user = new User("ali@example.com", "hashed", "Ali", Role.USER);
        when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest("ali@example.com", "secret123"));

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.user().email()).isEqualTo("ali@example.com");
    }

    @Test
    void loginFailsWithWrongPassword() {
        User user = new User("ali@example.com", "hashed", "Ali", Role.USER);
        when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ali@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginFailsForUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "secret123")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
