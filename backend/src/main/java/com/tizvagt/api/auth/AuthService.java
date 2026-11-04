package com.tizvagt.api.auth;

import com.tizvagt.api.auth.dto.AuthResponse;
import com.tizvagt.api.auth.dto.LoginRequest;
import com.tizvagt.api.auth.dto.RegisterRequest;
import com.tizvagt.api.security.JwtService;
import com.tizvagt.api.user.Role;
import com.tizvagt.api.user.User;
import com.tizvagt.api.user.UserRepository;
import com.tizvagt.api.user.UserResponse;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(), Role.USER);
        userRepository.save(user);
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                // Same message for unknown email and wrong password, so attackers can't probe for accounts.
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return AuthResponse.bearer(token, jwtService.getExpiration().toSeconds(), UserResponse.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
