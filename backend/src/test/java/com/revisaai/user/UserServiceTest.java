package com.revisaai.user;

import com.revisaai.question.Banca;
import com.revisaai.study.*;
import com.revisaai.user.dto.SessionSummary;
import com.revisaai.user.dto.UserStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private StudySessionRepository sessionRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(sessionRepository);
    }

    private StudySession buildSession(String userId, Banca banca, List<String> areas,
                                      List<Answer> answers, Resultado resultado) {
        var session = new StudySession(userId, banca, areas, answers.size(),
                SessionModo.ESTUDO, answers.stream().map(Answer::getQuestionId).toList());
        session.getAnswers().addAll(answers);
        session.setStatus(SessionStatus.FINALIZADA);
        session.setResultado(resultado);
        return session;
    }

    @Test
    @DisplayName("getStats sem sessões retorna todos os campos zerados")
    void getStats_semSessoes_retornaZeros() {
        given(sessionRepository.findByUserIdAndStatus("u1", SessionStatus.FINALIZADA))
                .willReturn(List.of());

        UserStatsResponse result = service.getStats("u1");

        assertThat(result.totalQuestoes()).isZero();
        assertThat(result.percentualAcertos()).isEqualTo(0.0);
        assertThat(result.totalSessoes()).isZero();
        assertThat(result.desempenhoPorArea()).isEmpty();
    }

    @Test
    @DisplayName("getStats totalQuestoes soma answers.size() de todas as sessões")
    void getStats_totalQuestoes_somaTodas() {
        var a1 = new Answer("q1", "CERTO", true, "Informática");
        var a2 = new Answer("q2", "ERRADO", false, "Informática");
        var a3 = new Answer("q3", "CERTO", true, "Português");

        var r1 = new Resultado(2, 1, 50.0, Map.of("Informática", 50.0));
        var r2 = new Resultado(1, 1, 100.0, Map.of("Português", 100.0));

        var s1 = buildSession("u1", Banca.CEBRASPE, List.of("Informática"), List.of(a1, a2), r1);
        var s2 = buildSession("u1", Banca.CEBRASPE, List.of("Português"), List.of(a3), r2);

        given(sessionRepository.findByUserIdAndStatus("u1", SessionStatus.FINALIZADA))
                .willReturn(List.of(s1, s2));

        UserStatsResponse result = service.getStats("u1");

        assertThat(result.totalQuestoes()).isEqualTo(3);
        assertThat(result.totalSessoes()).isEqualTo(2);
    }

    @Test
    @DisplayName("getStats percentualAcertos calcula (acertos/total)×100 globalmente")
    void getStats_percentualAcertos_calculaCorretamente() {
        var answers = List.of(
                new Answer("q1", "CERTO", true, "Informática"),
                new Answer("q2", "ERRADO", false, "Informática"),
                new Answer("q3", "CERTO", true, "Informática"),
                new Answer("q4", "ERRADO", false, "Informática")
        );
        var r = new Resultado(4, 2, 50.0, Map.of("Informática", 50.0));
        var session = buildSession("u1", Banca.CEBRASPE, List.of("Informática"), answers, r);

        given(sessionRepository.findByUserIdAndStatus("u1", SessionStatus.FINALIZADA))
                .willReturn(List.of(session));

        UserStatsResponse result = service.getStats("u1");

        assertThat(result.percentualAcertos()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getStats desempenhoPorArea calcula média por área somente entre sessões que a contêm")
    void getStats_desempenhoPorArea_mediaCorretaPorArea() {
        // Sessão 1: Informática 100%, Português 0%
        var r1 = new Resultado(2, 1, 50.0, Map.of("Informática", 100.0, "Português", 0.0));
        var s1 = buildSession("u1", Banca.CEBRASPE, List.of("Informática", "Português"),
                List.of(new Answer("q1", "CERTO", true, "Informática"),
                        new Answer("q2", "ERRADO", false, "Português")), r1);

        // Sessão 2: Informática 50% — Português ausente
        var r2 = new Resultado(2, 1, 50.0, Map.of("Informática", 50.0));
        var s2 = buildSession("u1", Banca.CEBRASPE, List.of("Informática"),
                List.of(new Answer("q3", "CERTO", true, "Informática"),
                        new Answer("q4", "ERRADO", false, "Informática")), r2);

        given(sessionRepository.findByUserIdAndStatus("u1", SessionStatus.FINALIZADA))
                .willReturn(List.of(s1, s2));

        UserStatsResponse result = service.getStats("u1");

        // Informática: média de 100.0 (s1) e 50.0 (s2) = 75.0
        assertThat(result.desempenhoPorArea().get("Informática")).isEqualTo(75.0);
        // Português: somente s1 tem a área → média de 0.0 = 0.0
        assertThat(result.desempenhoPorArea().get("Português")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getHistory retorna lista mapeada para SessionSummary na ordem recebida")
    void getHistory_retornaListaOrdenadaComoSessionSummary() {
        var r = new Resultado(2, 2, 100.0, Map.of("Informática", 100.0));
        var s = buildSession("u1", Banca.FGV, List.of("Informática"),
                List.of(new Answer("q1", "CERTO", true, "Informática"),
                        new Answer("q2", "CERTO", true, "Informática")), r);

        given(sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc("u1", SessionStatus.FINALIZADA))
                .willReturn(List.of(s));

        List<SessionSummary> result = service.getHistory("u1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).banca()).isEqualTo(Banca.FGV);
        assertThat(result.get(0).modo()).isEqualTo(SessionModo.ESTUDO);
        assertThat(result.get(0).quantidade()).isEqualTo(2);
        assertThat(result.get(0).resultado()).isNotNull();
        assertThat(result.get(0).resultado().getAcertos()).isEqualTo(2);
    }

    @Test
    @DisplayName("getHistory sem sessões retorna lista vazia")
    void getHistory_semSessoes_retornaListaVazia() {
        given(sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc("u1", SessionStatus.FINALIZADA))
                .willReturn(List.of());

        List<SessionSummary> result = service.getHistory("u1");

        assertThat(result).isEmpty();
    }
}
