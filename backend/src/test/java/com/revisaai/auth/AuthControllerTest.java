package com.revisaai.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisaai.auth.dto.AuthResponse;
import com.revisaai.auth.dto.LoginRequest;
import com.revisaai.auth.dto.RegisterRequest;
import com.revisaai.auth.oauth2.OAuth2FailureHandler;
import com.revisaai.auth.oauth2.OAuth2SuccessHandler;
import com.revisaai.auth.oauth2.OAuth2UserServiceImpl;
import com.revisaai.shared.exception.InvalidCredentialsException;
import com.revisaai.shared.exception.UserAlreadyExistsException;
import com.revisaai.shared.security.JwtService;
import com.revisaai.shared.security.SecurityConfig;
import com.revisaai.shared.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @Import traz SecurityConfig: CSRF off, auth endpoints em permitAll()
// @MockBean para deps de SecurityConfig e JwtAuthenticationFilter
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean AuthService authService;
    @MockBean OAuth2UserServiceImpl oauth2UserService;
    @MockBean OAuth2SuccessHandler oauth2SuccessHandler;
    @MockBean OAuth2FailureHandler oauth2FailureHandler;

    @Test
    @DisplayName("POST /auth/register com body válido deve retornar 201")
    void register_withValidBody_returns201() throws Exception {
        var request = new RegisterRequest("Ana", "ana@test.com", "senha123");
        var authResponse = new AuthResponse("tok.en.here", 900_000L);

        given(authService.register(any(), any())).willReturn(authResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("tok.en.here"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/register com e-mail inválido deve retornar 400")
    void register_withInvalidEmail_returns400() throws Exception {
        var request = new RegisterRequest("Ana", "not-an-email", "senha123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /auth/register com senha curta deve retornar 400")
    void register_withShortPassword_returns400() throws Exception {
        var request = new RegisterRequest("Ana", "ana@test.com", "123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("POST /auth/register com e-mail duplicado deve retornar 409")
    void register_withDuplicateEmail_returns409() throws Exception {
        var request = new RegisterRequest("Ana", "ana@test.com", "senha123");
        given(authService.register(any(), any()))
                .willThrow(new UserAlreadyExistsException("ana@test.com"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /auth/login com credenciais válidas deve retornar 200")
    void login_withValidCredentials_returns200() throws Exception {
        var request = new LoginRequest("ana@test.com", "senha123");
        var authResponse = new AuthResponse("tok.en.here", 900_000L);

        given(authService.login(any(), any())).willReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tok.en.here"));
    }

    @Test
    @DisplayName("POST /auth/login com credenciais inválidas deve retornar 401")
    void login_withInvalidCredentials_returns401() throws Exception {
        var request = new LoginRequest("ana@test.com", "errada");
        given(authService.login(any(), any()))
                .willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /auth/oauth2/exchange com code válido deve retornar 200")
    void exchange_withValidCode_returns200() throws Exception {
        given(authService.exchangeOAuth2Code(eq("valid-code"), any()))
                .willReturn(new AuthResponse("tok.en.here", 900_000L));

        mockMvc.perform(post("/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"valid-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tok.en.here"));
    }

    @Test
    @DisplayName("POST /auth/oauth2/exchange com code inválido deve retornar 401")
    void exchange_withInvalidCode_returns401() throws Exception {
        given(authService.exchangeOAuth2Code(eq("bad-code"), any()))
                .willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad-code\"}"))
                .andExpect(status().isUnauthorized());
    }
}
