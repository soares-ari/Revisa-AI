package com.revisaai.study;

import com.revisaai.question.Banca;
import com.revisaai.question.Dificuldade;
import com.revisaai.question.Question;
import com.revisaai.question.QuestionRepository;
import com.revisaai.shared.exception.InsufficientQuestionsException;
import com.revisaai.shared.exception.StudySessionForbiddenException;
import com.revisaai.shared.exception.StudySessionNotFoundException;
import com.revisaai.study.dto.AnswerRequest;
import com.revisaai.study.dto.CreateSessionRequest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyService")
class StudyServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private StudySessionRepository sessionRepository;

    @Mock
    private QuestionRepository questionRepository;

    private StudyService service;

    private Question q1;
    private Question q2;

    @BeforeEach
    void setUp() {
        service = new StudyService(mongoTemplate, sessionRepository, questionRepository);

        q1 = new Question("Enunciado 1", List.of("CERTO", "ERRADO"), "CERTO",
                Banca.CEBRASPE, 2023, "Analista", "Informática", Dificuldade.MEDIO, null);
        setId(q1, "q1");

        q2 = new Question("Enunciado 2", List.of("CERTO", "ERRADO"), "ERRADO",
                Banca.CEBRASPE, 2023, "Analista", "Português", Dificuldade.FACIL, null);
        setId(q2, "q2");

        lenient().when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setId(Question q, String id) {
        try {
            var field = Question.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(q, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mockAggregationResults(List<Question> questions) {
        var aggResults = new AggregationResults<>(questions, new Document());
        given(mongoTemplate.aggregate(any(Aggregation.class), eq("questions"), eq(Question.class)))
                .willReturn(aggResults);
    }

    @Test
    @DisplayName("create sem filtros cria sessão com questionIds corretos")
    void create_semFiltros_criaSessaoComQuestionIds() {
        mockAggregationResults(List.of(q1, q2));
        var request = new CreateSessionRequest(null, null, 2, SessionModo.ESTUDO);

        var result = service.create(request, "userId1");

        assertThat(result.getQuestionIds()).containsExactlyInAnyOrder("q1", "q2");
        assertThat(result.getStatus()).isEqualTo(SessionStatus.EM_ANDAMENTO);
        assertThat(result.getModo()).isEqualTo(SessionModo.ESTUDO);
        assertThat(result.getUserId()).isEqualTo("userId1");
        verify(sessionRepository).save(any(StudySession.class));
    }

    @Test
    @DisplayName("create com questões insuficientes lança InsufficientQuestionsException")
    void create_questoesInsuficientes_lancaExcecao() {
        mockAggregationResults(List.of(q1));
        var request = new CreateSessionRequest(null, null, 5, SessionModo.ESTUDO);

        assertThatThrownBy(() -> service.create(request, "userId1"))
                .isInstanceOf(InsufficientQuestionsException.class)
                .hasMessageContaining("solicitadas=5")
                .hasMessageContaining("disponíveis=1");
    }

    @Test
    @DisplayName("findById retorna sessão quando userId é o proprietário")
    void findById_proprietario_retornaSessao() {
        var session = new StudySession("userId1", null, null, 2, SessionModo.ESTUDO, List.of("q1"));
        given(sessionRepository.findById("s1")).willReturn(Optional.of(session));

        var result = service.findById("s1", "userId1");

        assertThat(result).isSameAs(session);
    }

    @Test
    @DisplayName("findById lança StudySessionForbiddenException quando userId é diferente")
    void findById_outroUsuario_lancaForbidden() {
        var session = new StudySession("userId1", null, null, 2, SessionModo.ESTUDO, List.of("q1"));
        given(sessionRepository.findById("s1")).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.findById("s1", "outroUsuario"))
                .isInstanceOf(StudySessionForbiddenException.class);
    }

    @Test
    @DisplayName("findById lança StudySessionNotFoundException quando sessão não existe")
    void findById_naoEncontrada_lancaNotFound() {
        given(sessionRepository.findById("nao-existe")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("nao-existe", "userId1"))
                .isInstanceOf(StudySessionNotFoundException.class);
    }

    @Test
    @DisplayName("answer com resposta correta em modo ESTUDO retorna AnswerResponse com gabarito")
    void answer_correta_modoEstudo_retornaComGabarito() {
        var session = new StudySession("userId1", null, null, 2, SessionModo.ESTUDO,
                List.of("q1", "q2"));
        given(sessionRepository.findById("s1")).willReturn(Optional.of(session));
        given(questionRepository.findById("q1")).willReturn(Optional.of(q1));

        var request = new AnswerRequest("q1", "CERTO");
        var response = service.answer("s1", request, "userId1");

        assertThat(response.correta()).isTrue();
        assertThat(response.gabarito()).isEqualTo("CERTO");
        assertThat(session.getAnswers()).hasSize(1);
        assertThat(session.getCurrentIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("answer com resposta errada em modo SIMULADO retorna AnswerResponse sem gabarito")
    void answer_errada_modoSimulado_retornaSemGabarito() {
        var session = new StudySession("userId1", null, null, 2, SessionModo.SIMULADO,
                List.of("q1", "q2"));
        given(sessionRepository.findById("s1")).willReturn(Optional.of(session));
        given(questionRepository.findById("q2")).willReturn(Optional.of(q2));

        var request = new AnswerRequest("q2", "CERTO");
        var response = service.answer("s1", request, "userId1");

        assertThat(response.correta()).isFalse();
        assertThat(response.gabarito()).isNull();
    }

    @Test
    @DisplayName("answer lança IllegalArgumentException quando sessão está FINALIZADA")
    void answer_sessaoFinalizada_lancaIllegalArgument() {
        var session = new StudySession("userId1", null, null, 2, SessionModo.ESTUDO,
                List.of("q1"));
        session.setStatus(SessionStatus.FINALIZADA);
        given(sessionRepository.findById("s1")).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.answer("s1", new AnswerRequest("q1", "CERTO"), "userId1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FINALIZADA");
    }

    @Test
    @DisplayName("answer lança IllegalArgumentException quando questão já foi respondida")
    void answer_questaoJaRespondida_lancaIllegalArgument() {
        var session = new StudySession("userId1", null, null, 2, SessionModo.ESTUDO,
                List.of("q1", "q2"));
        session.getAnswers().add(new Answer("q1", "CERTO", true, "Informática"));
        given(sessionRepository.findById("s1")).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.answer("s1", new AnswerRequest("q1", "ERRADO"), "userId1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já respondida");
    }

    @Test
    @DisplayName("answer lança IllegalArgumentException quando questionId não pertence à sessão")
    void answer_questaoForaDaSessao_lancaIllegalArgument() {
        var session = new StudySession("userId1", null, null, 2, SessionModo.ESTUDO,
                List.of("q1", "q2"));
        given(sessionRepository.findById("s1")).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.answer("s1", new AnswerRequest("qOutra", "CERTO"), "userId1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pertence");
    }

    @Test
    @DisplayName("finish calcula total, acertos e percentual corretos")
    void finish_calculaResultadoCorreto() {
        var session = new StudySession("userId1", null, null, 2, SessionModo.ESTUDO,
                List.of("q1", "q2"));
        session.getAnswers().add(new Answer("q1", "CERTO", true, "Informática"));
        session.getAnswers().add(new Answer("q2", "CERTO", false, "Português"));
        given(sessionRepository.findById("s1")).willReturn(Optional.of(session));

        var result = service.finish("s1", "userId1");

        assertThat(result.getStatus()).isEqualTo(SessionStatus.FINALIZADA);
        assertThat(result.getResultado().getTotal()).isEqualTo(2);
        assertThat(result.getResultado().getAcertos()).isEqualTo(1);
        assertThat(result.getResultado().getPercentual()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("finish calcula desempenhoPorArea por área")
    void finish_calculaDesempenhoPorArea() {
        var session = new StudySession("userId1", null, null, 3, SessionModo.ESTUDO,
                List.of("q1", "q2", "q3"));
        session.getAnswers().add(new Answer("q1", "CERTO", true, "Informática"));
        session.getAnswers().add(new Answer("q2", "ERRADO", false, "Informática"));
        session.getAnswers().add(new Answer("q3", "CERTO", true, "Português"));
        given(sessionRepository.findById("s1")).willReturn(Optional.of(session));

        var result = service.finish("s1", "userId1");

        var desempenho = result.getResultado().getDesempenhoPorArea();
        assertThat(desempenho.get("Informática")).isEqualTo(50.0);
        assertThat(desempenho.get("Português")).isEqualTo(100.0);
    }
}
