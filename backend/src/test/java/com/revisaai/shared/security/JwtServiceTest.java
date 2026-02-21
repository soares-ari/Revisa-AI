package com.revisaai.shared.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    // base64 de "test-secret-key-for-revisa-ai-testing-only!" (> 256 bits)
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1yZXZpc2EtYWktdGVzdGluZy1vbmx5IQ==";
    private static final long EXPIRATION_MS = 900_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateAccessToken deve retornar um JWT não-nulo e não-vazio")
    void generateAccessToken_returnsNonBlankJwt() {
        String token = jwtService.generateAccessToken("user-123", "user@test.com");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("extractUserId deve retornar o userId que foi usado na geração")
    void extractUserId_returnsCorrectUserId() {
        String token = jwtService.generateAccessToken("user-abc", "user@test.com");

        assertThat(jwtService.extractUserId(token)).isEqualTo("user-abc");
    }

    @Test
    @DisplayName("isTokenValid deve retornar true para um token recém-gerado")
    void isTokenValid_withFreshToken_returnsTrue() {
        String token = jwtService.generateAccessToken("user-123", "user@test.com");

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid deve retornar false para um token adulterado")
    void isTokenValid_withTamperedToken_returnsFalse() {
        String token = jwtService.generateAccessToken("user-123", "user@test.com");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("token expirado deve lançar JwtException ao extrair claims")
    void expiredToken_throwsJwtException() {
        JwtService shortLivedService = new JwtService(TEST_SECRET, 1L); // 1ms
        String token = shortLivedService.generateAccessToken("user-123", "user@test.com");

        // aguarda expiração
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThatThrownBy(() -> shortLivedService.extractUserId(token))
                .isInstanceOf(JwtException.class);
    }
}
