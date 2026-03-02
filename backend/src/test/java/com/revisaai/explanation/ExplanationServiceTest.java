package com.revisaai.explanation;

import com.revisaai.question.Banca;
import com.revisaai.question.Dificuldade;
import com.revisaai.question.Question;
import com.revisaai.question.QuestionRepository;
import com.revisaai.shared.exception.QuestionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExplanationService")
class ExplanationServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ExplanationRepository explanationRepository;

    private ExplanationService service;

    private Question question;

    @BeforeEach
    void setUp() {
        service = spy(new ExplanationService(null, questionRepository, explanationRepository));

        question = new Question("Julgue o item a seguir.",
                List.of("CERTO", "ERRADO"), "CERTO",
                Banca.CEBRASPE, 2023, "Analista", "Informática", Dificuldade.MEDIO);
        lenient().when(explanationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("getExplanation com cache hit retorna explicação existente sem chamar callClaude")
    void getExplanation_cacheHit_retornaExistente() {
        var cached = new Explanation("q1", "Explicação cacheada");
        given(explanationRepository.findByQuestionId("q1")).willReturn(Optional.of(cached));

        var result = service.getExplanation("q1");

        assertThat(result).isSameAs(cached);
        verify(service, never()).callClaude(anyString());
        verify(questionRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("getExplanation com cache miss gera explicação, persiste e retorna")
    void getExplanation_cacheMiss_geraEPersiste() {
        given(explanationRepository.findByQuestionId("q1")).willReturn(Optional.empty());
        given(questionRepository.findById("q1")).willReturn(Optional.of(question));
        doReturn("Explicação gerada pelo Claude").when(service).callClaude(anyString());

        var result = service.getExplanation("q1");

        assertThat(result.getQuestionId()).isEqualTo("q1");
        assertThat(result.getTexto()).isEqualTo("Explicação gerada pelo Claude");
        verify(explanationRepository).save(any(Explanation.class));
        verify(service).callClaude(anyString());
    }

    @Test
    @DisplayName("getExplanation com questionId inexistente lança QuestionNotFoundException")
    void getExplanation_questaoNaoEncontrada_lancaQuestionNotFoundException() {
        given(explanationRepository.findByQuestionId("nao-existe")).willReturn(Optional.empty());
        given(questionRepository.findById("nao-existe")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExplanation("nao-existe"))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessageContaining("nao-existe");
    }

    @Test
    @DisplayName("getExplanation quando Anthropic falha propaga exceção sem cachear")
    void getExplanation_anthropicFalha_propagaExcecao() {
        given(explanationRepository.findByQuestionId("q1")).willReturn(Optional.empty());
        given(questionRepository.findById("q1")).willReturn(Optional.of(question));
        doThrow(new RuntimeException("Anthropic timeout")).when(service).callClaude(anyString());

        assertThatThrownBy(() -> service.getExplanation("q1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Anthropic timeout");

        verify(explanationRepository, never()).save(any());
    }
}
