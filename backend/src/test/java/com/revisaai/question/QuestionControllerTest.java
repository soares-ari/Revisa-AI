package com.revisaai.question;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionController.class)
@Import(SecurityConfig.class)
@DisplayName("QuestionController")
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuestionService questionService;

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

    private final Question question = new Question(
            "A assertiva está correta?",
            List.of("CERTO", "ERRADO"),
            "CERTO",
            Banca.CEBRASPE,
            2023,
            "Analista de TI",
            "Informática",
            Dificuldade.MEDIO
    );

    @Test
    @DisplayName("GET /questions sem autenticação retorna 403")
    void getAll_semAutenticacao_retorna403() throws Exception {
        mockMvc.perform(get("/questions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /questions autenticado retorna 200 com lista de questões")
    void getAll_autenticado_retorna200ComLista() throws Exception {
        given(questionService.findAll(null, null, null)).willReturn(List.of(question));

        mockMvc.perform(get("/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enunciado").value("A assertiva está correta?"))
                .andExpect(jsonPath("$[0].banca").value("CEBRASPE"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /questions com filtros repassa parâmetros ao service")
    void getAll_comFiltros_repassaParamsAoService() throws Exception {
        given(questionService.findAll("CEBRASPE", "Informática", 2023))
                .willReturn(List.of(question));

        mockMvc.perform(get("/questions")
                        .param("banca", "CEBRASPE")
                        .param("area", "Informática")
                        .param("ano", "2023"))
                .andExpect(status().isOk());

        verify(questionService).findAll("CEBRASPE", "Informática", 2023);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /questions/{id} com id existente retorna 200 com a questão")
    void getById_idExistente_retorna200() throws Exception {
        given(questionService.findById("q1")).willReturn(question);

        mockMvc.perform(get("/questions/q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enunciado").value("A assertiva está correta?"))
                .andExpect(jsonPath("$.gabarito").value("CERTO"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /questions/{id} com id inexistente retorna 404")
    void getById_idInexistente_retorna404() throws Exception {
        given(questionService.findById("nao-existe"))
                .willThrow(new QuestionNotFoundException("nao-existe"));

        mockMvc.perform(get("/questions/nao-existe"))
                .andExpect(status().isNotFound());
    }
}
