package com.revisaai.explanation;

import com.revisaai.auth.oauth2.OAuth2FailureHandler;
import com.revisaai.auth.oauth2.OAuth2SuccessHandler;
import com.revisaai.auth.oauth2.OAuth2UserServiceImpl;
import com.revisaai.shared.exception.QuestionNotFoundException;
import com.revisaai.shared.security.JwtService;
import com.revisaai.shared.security.SecurityConfig;
import com.revisaai.shared.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExplanationController.class)
@Import(SecurityConfig.class)
@DisplayName("ExplanationController")
class ExplanationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExplanationService explanationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private OAuth2UserServiceImpl oauth2UserService;

    @MockBean
    private OAuth2SuccessHandler oauth2SuccessHandler;

    @MockBean
    private OAuth2FailureHandler oauth2FailureHandler;

    @Test
    @DisplayName("GET /explanations/:id sem autenticação retorna 403")
    void get_semAutenticacao_retorna403() throws Exception {
        mockMvc.perform(get("/explanations/q1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "userId1")
    @DisplayName("GET /explanations/:id autenticado retorna 200 com explicação")
    void get_questaoExistente_retorna200() throws Exception {
        var explanation = new Explanation("q1", "Texto da explicação gerada pelo Claude.");
        given(explanationService.getExplanation("q1")).willReturn(explanation);

        mockMvc.perform(get("/explanations/q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("q1"))
                .andExpect(jsonPath("$.texto").value("Texto da explicação gerada pelo Claude."));
    }

    @Test
    @WithMockUser(username = "userId1")
    @DisplayName("GET /explanations/:id retorna 404 quando questão não existe")
    void get_questaoNaoEncontrada_retorna404() throws Exception {
        given(explanationService.getExplanation("nao-existe"))
                .willThrow(new QuestionNotFoundException("nao-existe"));

        mockMvc.perform(get("/explanations/nao-existe"))
                .andExpect(status().isNotFound());
    }
}
