package com.revisaai.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisaai.auth.dto.AuthResponse;
import com.revisaai.auth.dto.LoginRequest;
import com.revisaai.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("QuestionIntegrationTest")
class QuestionIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri",
                () -> mongoDBContainer.getConnectionString() + "/revisaai_test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        questionRepository.deleteAll();

        var register = new RegisterRequest("Testador", "tester@test.com", "senha123");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        var login = new LoginRequest("tester@test.com", "senha123");
        var loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn();

        var authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);
        jwtToken = authResponse.accessToken();
    }

    @Test
    @DisplayName("GET /questions sem JWT retorna 403")
    void getAll_semJwt_retorna403() throws Exception {
        mockMvc.perform(get("/questions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /questions com JWT retorna 200 com lista de questões")
    void getAll_comJwt_retorna200ComQuestoes() throws Exception {
        questionRepository.saveAll(List.of(
                new Question("Enunciado 1", List.of("CERTO", "ERRADO"), "CERTO",
                        Banca.CEBRASPE, 2023, "Analista", "Informática", Dificuldade.MEDIO),
                new Question("Enunciado 2", List.of("A", "B", "C", "D", "E"), "A",
                        Banca.FGV, 2022, "Auditor", "Direito", Dificuldade.FACIL)
        ));

        mockMvc.perform(get("/questions")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /questions?banca=CEBRASPE retorna apenas questões da banca correta")
    void getAll_comFiltroBanca_retornaApenasBancaCorreta() throws Exception {
        questionRepository.saveAll(List.of(
                new Question("Enunciado CEBRASPE", List.of("CERTO", "ERRADO"), "CERTO",
                        Banca.CEBRASPE, 2023, "Analista", "Informática", Dificuldade.MEDIO),
                new Question("Enunciado FGV", List.of("A", "B", "C", "D", "E"), "A",
                        Banca.FGV, 2022, "Auditor", "Direito", Dificuldade.FACIL),
                new Question("Enunciado CESGRANRIO", List.of("A", "B", "C", "D", "E"), "C",
                        Banca.CESGRANRIO, 2021, "Técnico", "Matemática", Dificuldade.DIFICIL)
        ));

        mockMvc.perform(get("/questions")
                        .param("banca", "CEBRASPE")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].banca").value("CEBRASPE"));
    }

    @Test
    @DisplayName("GET /questions/{id} com id existente retorna 200 com a questão")
    void getById_idExistente_retornaQuestao() throws Exception {
        var saved = questionRepository.save(
                new Question("Questão específica", List.of("CERTO", "ERRADO"), "ERRADO",
                        Banca.CEBRASPE, 2023, "Analista", "Português", Dificuldade.FACIL));

        mockMvc.perform(get("/questions/" + saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enunciado").value("Questão específica"))
                .andExpect(jsonPath("$.gabarito").value("ERRADO"));
    }
}
