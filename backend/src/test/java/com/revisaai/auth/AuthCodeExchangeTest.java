package com.revisaai.auth;

import com.revisaai.auth.dto.AuthResponse;
import com.revisaai.auth.oauth2.AuthCode;
import com.revisaai.auth.oauth2.AuthCodeRepository;
import com.revisaai.shared.exception.InvalidCredentialsException;
import com.revisaai.shared.security.JwtService;
import com.revisaai.user.AuthProvider;
import com.revisaai.user.User;
import com.revisaai.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — OAuth2 Code Exchange")
class AuthCodeExchangeTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuthCodeRepository authCodeRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, refreshTokenRepository, authCodeRepository,
                jwtService, passwordEncoder, 604_800_000L, false
        );
    }

    @Test
    @DisplayName("code válido → retorna AuthResponse e deleta o code")
    void exchangeOAuth2Code_validCode_returnsTokenAndDeletesCode() {
        var user = new User("ana@test.com", null, "Ana", AuthProvider.GOOGLE);
        ReflectionTestUtils.setField(user, "id", "user-123");
        var authCode = new AuthCode("valid-code", "user-123", Instant.now().plusSeconds(60));

        given(authCodeRepository.findByCode("valid-code")).willReturn(Optional.of(authCode));
        given(userRepository.findById("user-123")).willReturn(Optional.of(user));
        given(jwtService.generateAccessToken(anyString(), anyString())).willReturn("access.token");
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        AuthResponse result = authService.exchangeOAuth2Code("valid-code", new MockHttpServletResponse());

        assertThat(result.accessToken()).isEqualTo("access.token");
        verify(authCodeRepository).deleteByCode("valid-code");
    }

    @Test
    @DisplayName("code inexistente → lança InvalidCredentialsException")
    void exchangeOAuth2Code_invalidCode_throwsInvalidCredentials() {
        given(authCodeRepository.findByCode("invalid")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.exchangeOAuth2Code("invalid", new MockHttpServletResponse()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("code expirado → deleta code e lança InvalidCredentialsException")
    void exchangeOAuth2Code_expiredCode_deletesAndThrows() {
        var authCode = new AuthCode("expired-code", "user-123", Instant.now().minusSeconds(10));
        given(authCodeRepository.findByCode("expired-code")).willReturn(Optional.of(authCode));

        assertThatThrownBy(() ->
                authService.exchangeOAuth2Code("expired-code", new MockHttpServletResponse()))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(authCodeRepository).deleteByCode("expired-code");
    }
}
