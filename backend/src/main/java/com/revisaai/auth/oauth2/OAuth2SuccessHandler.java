package com.revisaai.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final AuthCodeRepository authCodeRepository;
    private final String authorizedRedirectUri;

    public OAuth2SuccessHandler(
            AuthCodeRepository authCodeRepository,
            @Value("${app.oauth2.authorized-redirect-uri}") String authorizedRedirectUri) {
        this.authCodeRepository = authCodeRepository;
        this.authorizedRedirectUri = authorizedRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String userId = principal.getUser().getId();

        String code = UUID.randomUUID().toString();
        var authCode = new AuthCode(code, userId, Instant.now().plusSeconds(60));
        authCodeRepository.save(authCode);

        log.info("Auth code gerado para userId={}", userId);
        response.sendRedirect(authorizedRedirectUri + "?code=" + code);
    }
}
