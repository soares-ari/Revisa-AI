package com.revisaai.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisaai.question.Banca;
import com.revisaai.question.Question;
import com.revisaai.question.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionParserServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    private QuestionParserService service;

    private static final Banca BANCA = Banca.CEBRASPE;
    private static final Integer ANO = 2023;
    private static final String CARGO = "Analista";
    private static final String PROVA_ID = "prova-1";

    @BeforeEach
    void setUp() {
        service = spy(new QuestionParserService(null, questionRepository, new ObjectMapper()));
        lenient().when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void mockClaudeResponse(String json) {
        doReturn(json).when(service).callClaude(anyString());
    }

    @Test
    @DisplayName("parse com JSON válido salva todas as questões e retorna count correto")
    void parse_jsonValido_salvaQuestoes_retornaCountCorreto() {
        var json = """
                [
                  {
                    "enunciado": "Qual é a capital do Brasil?",
                    "alternativas": ["Brasília", "São Paulo", "Rio de Janeiro"],
                    "gabarito": "Brasília",
                    "area": "Geografia",
                    "dificuldade": "FACIL"
                  },
                  {
                    "enunciado": "Quem escreveu Dom Casmurro?",
                    "alternativas": ["Machado de Assis", "José de Alencar"],
                    "gabarito": "Machado de Assis",
                    "area": "Literatura",
                    "dificuldade": "MEDIO"
                  }
                ]
                """;
        mockClaudeResponse(json);

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesSalvas()).isEqualTo(2);
        assertThat(result.questoesInvalidas()).isEqualTo(0);

        var captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository, times(2)).save(captor.capture());
        // FALHA — stub passa null como provaId
        captor.getAllValues().forEach(q -> assertThat(q.getProvaId()).isEqualTo(PROVA_ID));
    }

    @Test
    @DisplayName("parse com gabarito fora das alternativas salva questão inválida e incrementa invalidas")
    void parse_gabaritoForaAlternativas_salvaInvalida_incrementaInvalidas() {
        var json = """
                [
                  {
                    "enunciado": "Questão com gabarito errado",
                    "alternativas": ["A", "B", "C"],
                    "gabarito": "D",
                    "area": "Informática",
                    "dificuldade": "DIFICIL"
                  }
                ]
                """;
        mockClaudeResponse(json);

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesSalvas()).isEqualTo(0);
        assertThat(result.questoesInvalidas()).isEqualTo(1);

        var captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.isValid()).isFalse();
        assertThat(saved.getValidationError()).isNotBlank();
        // FALHA — stub passa null como provaId
        assertThat(saved.getProvaId()).isEqualTo(PROVA_ID);
    }

    @Test
    @DisplayName("parse com JSON vazio retorna ParseResult(0, 0) sem salvar nada")
    void parse_jsonVazio_retornaZerosSemSalvar() {
        mockClaudeResponse("[]");

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesSalvas()).isEqualTo(0);
        assertThat(result.questoesInvalidas()).isEqualTo(0);
        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("parse com JSON malformado lança RuntimeException")
    void parse_jsonMalformado_lancaRuntimeException() {
        mockClaudeResponse("isso não é json válido");

        assertThatThrownBy(() ->
                service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("parse com dificuldade desconhecida salva questão inválida com validationError")
    void parse_dificuldadeDesconhecida_salvaComValidationError() {
        var json = """
                [
                  {
                    "enunciado": "Questão com dificuldade absurda",
                    "alternativas": ["A", "B"],
                    "gabarito": "A",
                    "area": "Português",
                    "dificuldade": "ABSURDA"
                  }
                ]
                """;
        mockClaudeResponse(json);

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesInvalidas()).isEqualTo(1);
        var captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().isValid()).isFalse();
        assertThat(captor.getValue().getValidationError()).contains("ABSURDA");
    }
}
