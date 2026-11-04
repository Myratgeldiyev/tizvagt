package com.tizvagt.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tizvagt.api.user.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtdGhhdC1pcy1hdC1sZWFzdC0zMi1ieXRlcy1sb25n";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofHours(1));
    private final JwtService jwtService = new JwtService(properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void generatedTokenContainsEmail() {
        String token = jwtService.generateToken("ali@example.com", Role.USER);

        assertThat(jwtService.extractEmail(token)).contains("ali@example.com");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken("ali@example.com", Role.USER);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.extractEmail(tampered)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        var otherKey = new JwtProperties("b3RoZXItc2VjcmV0LXRoYXQtaXMtYXQtbGVhc3QtMzItYnl0ZXMtbG9uZw==", Duration.ofHours(1));
        String foreignToken = new JwtService(otherKey, Clock.fixed(NOW, ZoneOffset.UTC))
                .generateToken("ali@example.com", Role.USER);

        assertThat(jwtService.extractEmail(foreignToken)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.generateToken("ali@example.com", Role.USER);
        var twoHoursLater = new JwtService(properties, Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC));

        assertThat(twoHoursLater.extractEmail(token)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(jwtService.extractEmail("not-a-jwt")).isEmpty();
    }
}
