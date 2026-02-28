package com.revisaai.question;

import com.revisaai.shared.exception.QuestionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService")
class QuestionServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private QuestionRepository questionRepository;

    private QuestionService service;

    private Question cebraspe;
    private Question fgv;

    @BeforeEach
    void setUp() {
        service = new QuestionService(mongoTemplate, questionRepository);
        cebraspe = new Question("Enunciado 1", List.of("CERTO", "ERRADO"), "CERTO",
                Banca.CEBRASPE, 2023, "Analista de TI", "Informática", Dificuldade.MEDIO);
        fgv = new Question("Enunciado 2", List.of("A", "B", "C", "D", "E"), "B",
                Banca.FGV, 2022, "Auditor", "Direito", Dificuldade.DIFICIL);
    }

    @Test
    @DisplayName("findAll sem filtros retorna todas as questões")
    void findAll_semFiltros_retornaTodasQuestoes() {
        given(mongoTemplate.find(any(Query.class), eq(Question.class)))
                .willReturn(List.of(cebraspe, fgv));

        var result = service.findAll(null, null, null);

        assertThat(result).hasSize(2);
        verify(mongoTemplate).find(any(Query.class), eq(Question.class));
    }

    @Test
    @DisplayName("findAll com banca aplica filtro de banca na query")
    void findAll_comBanca_aplicaFiltroBanca() {
        given(mongoTemplate.find(any(Query.class), eq(Question.class)))
                .willReturn(List.of(cebraspe));

        var queryCaptor = ArgumentCaptor.forClass(Query.class);

        var result = service.findAll("CEBRASPE", null, null);

        verify(mongoTemplate).find(queryCaptor.capture(), eq(Question.class));
        assertThat(result).hasSize(1);
        assertThat(queryCaptor.getValue().getQueryObject().containsKey("banca")).isTrue();
    }

    @Test
    @DisplayName("findAll com area e ano aplica múltiplos filtros")
    void findAll_comAreaEAno_aplicaFiltrosMultiplos() {
        given(mongoTemplate.find(any(Query.class), eq(Question.class)))
                .willReturn(List.of(cebraspe));

        var queryCaptor = ArgumentCaptor.forClass(Query.class);

        service.findAll(null, "Informática", 2023);

        verify(mongoTemplate).find(queryCaptor.capture(), eq(Question.class));
        var queryDoc = queryCaptor.getValue().getQueryObject();
        assertThat(queryDoc.containsKey("area")).isTrue();
        assertThat(queryDoc.containsKey("ano")).isTrue();
    }

    @Test
    @DisplayName("findAll com banca inválida lança IllegalArgumentException")
    void findAll_comBancaInvalida_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.findAll("INVALIDA", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findById com id existente retorna a questão")
    void findById_idExistente_retornaQuestao() {
        given(questionRepository.findById("q1")).willReturn(Optional.of(cebraspe));

        var result = service.findById("q1");

        assertThat(result.getEnunciado()).isEqualTo("Enunciado 1");
    }

    @Test
    @DisplayName("findById com id inexistente lança QuestionNotFoundException")
    void findById_idInexistente_throwsQuestionNotFoundException() {
        given(questionRepository.findById("nao-existe")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("nao-existe"))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessageContaining("nao-existe");
    }

    @Test
    @DisplayName("Question com gabarito fora das alternativas lança IllegalArgumentException")
    void question_gabaritoForaDasAlternativas_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                new Question("Enunciado", List.of("CERTO", "ERRADO"), "INCORRETO",
                        Banca.CEBRASPE, 2023, "Cargo", "Área", Dificuldade.FACIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INCORRETO");
    }
}
