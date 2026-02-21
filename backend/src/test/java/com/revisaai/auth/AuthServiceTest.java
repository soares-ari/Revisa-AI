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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register com e-mail novo deve salvar usuário e retornar token")
    void register_withNewEmail_savesUserAndReturnsToken() {
        var request = new RegisterRequest("Ana", "ana@test.com", "senha123");
        given(userRepository.existsByEmail("ana@test.com")).willReturn(false);
        given(passwordEncoder.encode("senha123")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> {
            User u = inv.getArgument(0);
            return u;
        });
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(jwtService.generateAccessToken(any(), anyString())).willReturn("access.token.here");

        AuthResponse response = authService.register(request, new MockHttpServletResponse());

        assertThat(response.accessToken()).isEqualTo("access.token.here");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register com e-mail duplicado deve lançar UserAlreadyExistsException")
    void register_withExistingEmail_throwsUserAlreadyExistsException() {
        var request = new RegisterRequest("Ana", "ana@test.com", "senha123");
        given(userRepository.existsByEmail("ana@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request, new MockHttpServletResponse()))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    @DisplayName("login com credenciais válidas deve retornar token e setar cookie")
    void login_withValidCredentials_returnsTokenAndSetsCookie() {
        var request = new LoginRequest("ana@test.com", "senha123");
        var user = new User("ana@test.com", "hashed", "Ana", AuthProvider.EMAIL);
        given(userRepository.findByEmail("ana@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("senha123", "hashed")).willReturn(true);
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(jwtService.generateAccessToken(any(), anyString())).willReturn("access.token.here");

        var response = new MockHttpServletResponse();
        AuthResponse result = authService.login(request, response);

        assertThat(result.accessToken()).isEqualTo("access.token.here");
        assertThat(response.getCookie("refreshToken")).isNotNull();
        assertThat(response.getCookie("refreshToken").isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("login com e-mail inexistente deve lançar InvalidCredentialsException")
    void login_withUnknownEmail_throwsInvalidCredentialsException() {
        var request = new LoginRequest("naoexiste@test.com", "senha123");
        given(userRepository.findByEmail("naoexiste@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, new MockHttpServletResponse()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login com senha errada deve lançar InvalidCredentialsException")
    void login_withWrongPassword_throwsInvalidCredentialsException() {
        var request = new LoginRequest("ana@test.com", "errada");
        var user = new User("ana@test.com", "hashed", "Ana", AuthProvider.EMAIL);
        given(userRepository.findByEmail("ana@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("errada", "hashed")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request, new MockHttpServletResponse()))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
