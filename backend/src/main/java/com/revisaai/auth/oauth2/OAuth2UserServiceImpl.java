package com.revisaai.auth.oauth2;

import com.revisaai.user.AuthProvider;
import com.revisaai.user.User;
import com.revisaai.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2UserServiceImpl.class);

    private final UserRepository userRepository;

    public OAuth2UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();
        return processUser(registrationId, oauth2User.getAttributes());
    }

    // package-private para teste direto sem mock da superclasse
    UserPrincipal processUser(String registrationId, Map<String, Object> attributes) {
        OAuth2UserInfo userInfo;
        if ("google".equals(registrationId)) {
            userInfo = new GoogleOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("provider_not_supported"),
                    "Provider " + registrationId + " not supported"
            );
        }

        String email = userInfo.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Email not found in OAuth2 response"
            );
        }

        User user = userRepository.findByEmail(email)
                .map(existing -> updateUser(existing, userInfo))
                .orElseGet(() -> createUser(userInfo));

        return new UserPrincipal(user, attributes);
    }

    private User createUser(OAuth2UserInfo userInfo) {
        var user = new User(userInfo.getEmail(), null, userInfo.getName(), AuthProvider.GOOGLE);
        user.setPictureUrl(userInfo.getPictureUrl());
        User saved = userRepository.save(user);
        log.info("Novo usuário Google registrado: {}", saved.getEmail());
        return saved;
    }

    private User updateUser(User existing, OAuth2UserInfo userInfo) {
        existing.setName(userInfo.getName());
        existing.setPictureUrl(userInfo.getPictureUrl());
        existing.setProviderId(userInfo.getId());
        User saved = userRepository.save(existing);
        log.info("Usuário Google atualizado: {}", saved.getEmail());
        return saved;
    }
}
