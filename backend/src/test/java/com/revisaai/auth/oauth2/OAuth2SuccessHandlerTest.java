package com.revisaai.auth.oauth2;

import com.revisaai.user.AuthProvider;
import com.revisaai.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2SuccessHandler")
class OAuth2SuccessHandlerTest {

    @Mock
    private AuthCodeRepository authCodeRepository;

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(authCodeRepository, "http://localhost:5173");
    }

    private Authentication mockAuth(User user) {
        var principal = new UserPrincipal(user, Map.of());
        var auth = mock(Authentication.class);
        given(auth.getPrincipal()).willReturn(principal);
        return auth;
    }

    @Test
    @DisplayName("redirect URL contém parâmetro code (não token)")
    void onSuccess_redirectContainsCodeParam() throws Exception {
        var user = new User("ana@test.com", null, "Ana", AuthProvider.GOOGLE);
        ReflectionTestUtils.setField(user, "id", "user-123");
        given(authCodeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, mockAuth(user));

        assertThat(response.getRedirectedUrl()).startsWith("http://localhost:5173");
        assertThat(response.getRedirectedUrl()).contains("code=");
        assertThat(response.getRedirectedUrl()).doesNotContain("token=");
    }

    @Test
    @DisplayName("authCodeRepository.save() chamado com userId correto")
    void onSuccess_savesAuthCodeWithCorrectUserId() throws Exception {
        var user = new User("ana@test.com", null, "Ana", AuthProvider.GOOGLE);
        ReflectionTestUtils.setField(user, "id", "user-123");
        var captor = ArgumentCaptor.forClass(AuthCode.class);
        given(authCodeRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(), new MockHttpServletResponse(), mockAuth(user));

        assertThat(captor.getValue().getUserId()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("handler não emite cookie refreshToken")
    void onSuccess_doesNotSetRefreshTokenCookie() throws Exception {
        var user = new User("ana@test.com", null, "Ana", AuthProvider.GOOGLE);
        ReflectionTestUtils.setField(user, "id", "user-123");
        given(authCodeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, mockAuth(user));

        assertThat(response.getCookie("refreshToken")).isNull();
    }
}
