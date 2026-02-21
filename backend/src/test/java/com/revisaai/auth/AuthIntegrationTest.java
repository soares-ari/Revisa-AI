package com.revisaai.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisaai.auth.dto.LoginRequest;
import com.revisaai.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Auth — Integration Tests")
class AuthIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri",
                () -> mongoDBContainer.getConnectionString() + "/revisaai_test");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired com.revisaai.user.UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
        refreshTokenRepository.deleteAll();
    }

    @Test
    @DisplayName("fluxo completo: register → login → refresh deve funcionar")
    void fullAuthFlow_registerLoginRefresh_succeeds() throws Exception {
        var registerRequest = new RegisterRequest("Ana Silva", "ana@test.com", "senha123");

        // 1. Registro
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()));

        // 2. Login
        var loginRequest = new LoginRequest("ana@test.com", "senha123");
        var loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn();

        String setCookie = loginResult.getResponse().getHeader("Set-Cookie");
        String refreshTokenCookie = setCookie != null ? setCookie.split(";")[0] : "";

        // 3. Refresh
        mockMvc.perform(post("/auth/refresh")
                        .header("Cookie", refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("register com e-mail duplicado deve retornar 409")
    void register_duplicateEmail_returns409() throws Exception {
        var request = new RegisterRequest("Ana Silva", "ana@test.com", "senha123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("login com credenciais inválidas deve retornar 401")
    void login_invalidCredentials_returns401() throws Exception {
        var request = new LoginRequest("ninguem@test.com", "qualquercoisa");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("endpoint protegido sem token deve retornar 403")
    void protectedEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/questions"))
                .andExpect(status().isForbidden());
    }
}
