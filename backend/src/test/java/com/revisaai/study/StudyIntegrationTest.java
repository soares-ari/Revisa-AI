package com.revisaai.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisaai.auth.dto.AuthResponse;
import com.revisaai.auth.dto.LoginRequest;
import com.revisaai.auth.dto.RegisterRequest;
import com.revisaai.ingestion.QuestionParserService;
import com.revisaai.question.Banca;
import com.revisaai.question.Dificuldade;
import com.revisaai.question.Question;
import com.revisaai.question.QuestionRepository;
import com.revisaai.study.dto.AnswerRequest;
import com.revisaai.study.dto.CreateSessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class StudyIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri",
                () -> mongoDBContainer.getConnectionString() + "/revisaai_test");
    }

    @MockBean
    private QuestionParserService questionParserService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        studySessionRepository.deleteAll();
        questionRepository.deleteAll();

        var register = new RegisterRequest("Estudante", "estudante@test.com", "senha123");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        var login = new LoginRequest("estudante@test.com", "senha123");
        var loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn();

        var authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);
        jwtToken = authResponse.accessToken();

        questionRepository.save(new Question("Questão 1", List.of("CERTO", "ERRADO"), "CERTO",
                Banca.CEBRASPE, 2023, "Analista", "Informática", Dificuldade.MEDIO, null));
        questionRepository.save(new Question("Questão 2", List.of("CERTO", "ERRADO"), "ERRADO",
                Banca.CEBRASPE, 2023, "Analista", "Informática", Dificuldade.FACIL, null));
    }

    @Test
    @DisplayName("POST /study/sessions retorna 400 quando questões insuficientes")
    void post_questoesInsuficientes_retorna400() throws Exception {
        var body = new CreateSessionRequest(null, null, 10, SessionModo.ESTUDO);

        mockMvc.perform(post("/study/sessions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /study/sessions retorna 201 com questionIds quando há questões suficientes")
    void post_questoesSuficientes_retorna201() throws Exception {
        var body = new CreateSessionRequest(null, null, 2, SessionModo.ESTUDO);

        mockMvc.perform(post("/study/sessions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionIds", hasSize(2)))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("Fluxo completo: criar sessão → responder → finalizar retorna resultado")
    void fluxoCompleto_criarResponderFinalizar() throws Exception {
        var body = new CreateSessionRequest(null, null, 2, SessionModo.ESTUDO);

        var createResult = mockMvc.perform(post("/study/sessions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        var session = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), StudySession.class);
        var sessionId = session.getId();
        var questionIds = session.getQuestionIds();

        for (String qId : questionIds) {
            var answerBody = new AnswerRequest(qId, "CERTO");
            mockMvc.perform(post("/study/sessions/" + sessionId + "/answer")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(answerBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionId").value(qId));
        }

        mockMvc.perform(post("/study/sessions/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.resultado.total").value(2))
                .andExpect(jsonPath("$.resultado.percentual").isNumber());

        var saved = studySessionRepository.findById(sessionId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.FINALIZADA);
        assertThat(saved.getResultado()).isNotNull();
    }
}
