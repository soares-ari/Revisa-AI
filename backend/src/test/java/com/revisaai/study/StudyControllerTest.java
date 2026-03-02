package com.revisaai.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisaai.auth.oauth2.OAuth2FailureHandler;
import com.revisaai.auth.oauth2.OAuth2SuccessHandler;
import com.revisaai.auth.oauth2.OAuth2UserServiceImpl;
import com.revisaai.shared.exception.StudySessionForbiddenException;
import com.revisaai.shared.security.JwtService;
import com.revisaai.shared.security.SecurityConfig;
import com.revisaai.shared.security.UserDetailsServiceImpl;
import com.revisaai.study.dto.AnswerRequest;
import com.revisaai.study.dto.AnswerResponse;
import com.revisaai.study.dto.CreateSessionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudyController.class)
@Import(SecurityConfig.class)
@DisplayName("StudyController")
class StudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudyService studyService;

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

    private StudySession buildSession(SessionModo modo) {
        var s = new StudySession("userId1", null, null, 2, modo, List.of("q1", "q2"));
        return s;
    }

    @Test
    @DisplayName("POST /study/sessions sem autenticação retorna 403")
    void post_semAutenticacao_retorna403() throws Exception {
        mockMvc.perform(post("/study/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "userId1")
    @DisplayName("POST /study/sessions autenticado retorna 201 com sessão")
    void post_autenticado_retorna201() throws Exception {
        var session = buildSession(SessionModo.ESTUDO);
        given(studyService.create(any(CreateSessionRequest.class), eq("userId1")))
                .willReturn(session);

        var body = new CreateSessionRequest(null, null, 2, SessionModo.ESTUDO);

        mockMvc.perform(post("/study/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.modo").value("ESTUDO"));
    }

    @Test
    @WithMockUser(username = "userId1")
    @DisplayName("GET /study/sessions/:id retorna 200 com sessão existente")
    void get_sessaoExistente_retorna200() throws Exception {
        given(studyService.findById(eq("s1"), eq("userId1")))
                .willReturn(buildSession(SessionModo.ESTUDO));

        mockMvc.perform(get("/study/sessions/s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("userId1"));
    }

    @Test
    @WithMockUser(username = "outroUsuario")
    @DisplayName("GET /study/sessions/:id retorna 403 quando userId não bate")
    void get_outroUsuario_retorna403() throws Exception {
        given(studyService.findById(eq("s1"), eq("outroUsuario")))
                .willThrow(new StudySessionForbiddenException());

        mockMvc.perform(get("/study/sessions/s1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "userId1")
    @DisplayName("POST .../answer em modo ESTUDO retorna 200 com gabarito")
    void postAnswer_modoEstudo_retorna200ComGabarito() throws Exception {
        var answerResp = new AnswerResponse("q1", "CERTO", true, "Informática", "CERTO");
        given(studyService.answer(eq("s1"), any(AnswerRequest.class), eq("userId1")))
                .willReturn(answerResp);

        var reqBody = new AnswerRequest("q1", "CERTO");

        mockMvc.perform(post("/study/sessions/s1/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correta").value(true))
                .andExpect(jsonPath("$.gabarito").value("CERTO"));
    }

    @Test
    @WithMockUser(username = "userId1")
    @DisplayName("POST .../answer em modo SIMULADO retorna 200 sem gabarito")
    void postAnswer_modoSimulado_retorna200SemGabarito() throws Exception {
        var answerResp = new AnswerResponse("q2", "CERTO", false, "Português", null);
        given(studyService.answer(eq("s1"), any(AnswerRequest.class), eq("userId1")))
                .willReturn(answerResp);

        var reqBody = new AnswerRequest("q2", "CERTO");

        mockMvc.perform(post("/study/sessions/s1/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correta").value(false))
                .andExpect(jsonPath("$.gabarito").doesNotExist());
    }

    @Test
    @WithMockUser(username = "userId1")
    @DisplayName("POST .../finish retorna 200 com sessão e resultado")
    void postFinish_retorna200ComResultado() throws Exception {
        var session = buildSession(SessionModo.ESTUDO);
        session.setStatus(SessionStatus.FINALIZADA);
        session.setResultado(new Resultado(2, 1, 50.0, java.util.Map.of("Informática", 50.0)));
        given(studyService.finish(eq("s1"), eq("userId1"))).willReturn(session);

        mockMvc.perform(post("/study/sessions/s1/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.resultado.total").value(2))
                .andExpect(jsonPath("$.resultado.acertos").value(1))
                .andExpect(jsonPath("$.resultado.percentual").value(50.0));
    }
}
