package com.revisaai.auth;

import com.revisaai.auth.dto.AuthResponse;
import com.revisaai.auth.dto.LoginRequest;
import com.revisaai.auth.dto.RegisterRequest;
import com.revisaai.shared.exception.InvalidCredentialsException;
import com.revisaai.shared.exception.UserAlreadyExistsException;
import com.revisaai.shared.security.JwtService;
import com.revisaai.user.AuthProvider;
import com.revisaai.user.User;
import com.revisaai.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTokenExpirationMs;
    private final boolean cookieSecure;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            @Value("${app.refresh-token.expiration}") long refreshTokenExpirationMs,
            @Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.cookieSecure = cookieSecure;
    }

    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        var user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                AuthProvider.EMAIL
        );
        User saved = userRepository.save(user);
        log.info("Novo usuário registrado: {}", saved.getEmail());

        return issueTokens(saved, response);
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        log.info("Login realizado: {}", user.getEmail());
        return issueTokens(user, response);
    }

    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawCookie = request.getHeader("Cookie");
        String refreshTokenValue = extractRefreshTokenFromCookieHeader(rawCookie);

        if (refreshTokenValue == null) {
            throw new InvalidCredentialsException();
        }

        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(InvalidCredentialsException::new);

        if (stored.isExpired()) {
            refreshTokenRepository.deleteByToken(refreshTokenValue);
            log.warn("Refresh token expirado para userId={}", stored.getUserId());
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(InvalidCredentialsException::new);

        // rotação: invalida o antigo, emite novo
        refreshTokenRepository.deleteByToken(refreshTokenValue);
        log.info("Refresh token rotacionado para userId={}", user.getId());

        return issueTokens(user, response);
    }

    private AuthResponse issueTokens(User user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        String rawRefreshToken = UUID.randomUUID().toString();
        var refreshToken = new RefreshToken(
                rawRefreshToken,
                user.getId(),
                Instant.now().plusMillis(refreshTokenExpirationMs)
        );
        refreshTokenRepository.save(refreshToken);

        setRefreshCookie(response, rawRefreshToken);

        return new AuthResponse(accessToken, jwtService.getExpirationMs());
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/auth/refresh")
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractRefreshTokenFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null) return null;
        return Arrays.stream(cookieHeader.split(";"))
                .map(String::trim)
                .filter(c -> c.startsWith(REFRESH_COOKIE_NAME + "="))
                .map(c -> c.substring(REFRESH_COOKIE_NAME.length() + 1))
                .findFirst()
                .orElse(null);
    }
}
