package com.revisaai.auth.oauth2;

import com.revisaai.user.AuthProvider;
import com.revisaai.user.User;
import com.revisaai.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2UserServiceImpl")
class OAuth2UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private OAuth2UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OAuth2UserServiceImpl(userRepository);
    }

    @Test
    @DisplayName("novo usuário Google → salva e retorna UserPrincipal com provider GOOGLE")
    void processUser_newGoogleUser_savesAndReturnsPrincipal() {
        Map<String, Object> attrs = Map.of(
                "sub", "google-123",
                "name", "Ana",
                "email", "ana@test.com",
                "picture", "http://pic.png"
        );
        given(userRepository.findByEmail("ana@test.com")).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        UserPrincipal result = service.processUser("google", attrs);

        assertThat(result.getUser().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(result.getUser().getEmail()).isEqualTo("ana@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("e-mail já existe → atualiza nome e foto e salva usuário existente")
    void processUser_existingEmail_updatesNameAndPicture() {
        var existing = new User("ana@test.com", null, "Ana Antiga", AuthProvider.EMAIL);
        Map<String, Object> attrs = Map.of(
                "sub", "google-123",
                "name", "Ana Nova",
                "email", "ana@test.com",
                "picture", "http://new-pic.png"
        );
        given(userRepository.findByEmail("ana@test.com")).willReturn(Optional.of(existing));
        given(userRepository.save(existing)).willReturn(existing);

        UserPrincipal result = service.processUser("google", attrs);

        assertThat(result.getUser().getName()).isEqualTo("Ana Nova");
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("provider não suportado → OAuth2AuthenticationException")
    void processUser_unsupportedProvider_throwsException() {
        Map<String, Object> attrs = Map.of("sub", "gh-123", "name", "Ana", "email", "ana@test.com");

        assertThatThrownBy(() -> service.processUser("github", attrs))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    @DisplayName("e-mail ausente nos attributes → OAuth2AuthenticationException")
    void processUser_missingEmail_throwsException() {
        Map<String, Object> attrs = Map.of("sub", "google-123", "name", "Ana");

        assertThatThrownBy(() -> service.processUser("google", attrs))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
