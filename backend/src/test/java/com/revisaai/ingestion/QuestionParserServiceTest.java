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
import static org.mockito.ArgumentMatchers.*;
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
        lenient().when(questionRepository.existsByProvaIdAndEnunciado(anyString(), anyString())).thenReturn(false);
    }

    private void mockCount(int total) {
        doReturn(total).when(service).callClaudeCount(anyString());
    }

    private void mockRange(String json) {
        doReturn(json).when(service).callClaudeRange(anyString(), anyString(), anyInt(), anyInt());
    }

    /** Gera array JSON com questões numeradas de inicio a fim (alternativas válidas A/B, gabarito A). */
    private String buildJsonArray(int inicio, int fim) {
        var sb = new StringBuilder("[");
        for (int i = inicio; i <= fim; i++) {
            if (i > inicio) sb.append(",");
            sb.append("{\"numero\":%d,\"enunciado\":\"Questão %d\",\"alternativas\":[\"A\",\"B\"],\"gabarito\":\"A\",\"area\":\"TI\",\"dificuldade\":\"FACIL\"}"
                    .formatted(i, i));
        }
        return sb.append("]").toString();
    }

    // ── testes existentes migrados ──────────────────────────────────────────────

    @Test
    @DisplayName("parse com JSON válido salva todas as questões e retorna count correto")
    void parse_jsonValido_salvaQuestoes_retornaCountCorreto() {
        mockCount(2);
        mockRange("""
                [
                  {"numero":1,"enunciado":"Qual é a capital do Brasil?","alternativas":["Brasília","São Paulo","Rio de Janeiro"],"gabarito":"Brasília","area":"Geografia","dificuldade":"FACIL"},
                  {"numero":2,"enunciado":"Quem escreveu Dom Casmurro?","alternativas":["Machado de Assis","José de Alencar"],"gabarito":"Machado de Assis","area":"Literatura","dificuldade":"MEDIO"}
                ]
                """);

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesSalvas()).isEqualTo(2);
        assertThat(result.questoesInvalidas()).isEqualTo(0);

        var captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository, times(2)).save(captor.capture());
        captor.getAllValues().forEach(q -> assertThat(q.getProvaId()).isEqualTo(PROVA_ID));
    }

    @Test
    @DisplayName("parse com gabarito fora das alternativas salva questão inválida e incrementa invalidas")
    void parse_gabaritoForaAlternativas_salvaInvalida_incrementaInvalidas() {
        mockCount(1);
        mockRange("""
                [{"numero":1,"enunciado":"Questão com gabarito errado","alternativas":["A","B","C"],"gabarito":"D","area":"Informática","dificuldade":"DIFICIL"}]
                """);

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesSalvas()).isEqualTo(0);
        assertThat(result.questoesInvalidas()).isEqualTo(1);

        var captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.isValid()).isFalse();
        assertThat(saved.getValidationError()).isNotBlank();
        assertThat(saved.getProvaId()).isEqualTo(PROVA_ID);
    }

    @Test
    @DisplayName("parse com contagem zero retorna ParseResult(0, 0) sem salvar nada")
    void parse_contagemZero_retornaZerosSemSalvar() {
        mockCount(0);

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesSalvas()).isEqualTo(0);
        assertThat(result.questoesInvalidas()).isEqualTo(0);
        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("parse com JSON malformado em todos os batches retorna resultado parcial sem questões")
    void parse_jsonMalformado_retornaResultadoParcialSemQuestoes() {
        mockCount(1);
        mockRange("isso não é json válido");

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesSalvas()).isEqualTo(0);
        assertThat(result.partialErrorMessage()).isNotNull();
    }

    @Test
    @DisplayName("parse com dificuldade desconhecida salva questão inválida com validationError")
    void parse_dificuldadeDesconhecida_salvaComValidationError() {
        mockCount(1);
        mockRange("""
                [{"numero":1,"enunciado":"Questão com dificuldade absurda","alternativas":["A","B"],"gabarito":"A","area":"Português","dificuldade":"ABSURDA"}]
                """);

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesInvalidas()).isEqualTo(1);
        var captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().isValid()).isFalse();
        assertThat(captor.getValue().getValidationError()).contains("ABSURDA");
    }

    // ── novos testes RED ────────────────────────────────────────────────────────

    @Test
    @DisplayName("parse com 25 questões faz 2 chamadas de extração por range")
    void parse_provaComMaisDeUmBatch_fazDuasChamadasDeExtracao() {
        mockCount(25);
        doReturn(buildJsonArray(1, 20))
                .doReturn(buildJsonArray(21, 25))
                .when(service).callClaudeRange(anyString(), anyString(), anyInt(), anyInt());

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        verify(service, times(2)).callClaudeRange(anyString(), anyString(), anyInt(), anyInt());
        verify(service).callClaudeRange(anyString(), anyString(), eq(1), eq(QuestionParserService.BATCH_SIZE));
        verify(service).callClaudeRange(anyString(), anyString(), eq(QuestionParserService.BATCH_SIZE + 1), eq(25));
        assertThat(result.questoesSalvas()).isEqualTo(25);
        assertThat(result.partialErrorMessage()).isNull();
    }

    @Test
    @DisplayName("parse realiza retry para questões faltantes e cobre a prova inteira")
    void parse_coberturaContinuaAposRetry() {
        mockCount(20);
        // Primeiro batch retorna apenas questões 1-15; retry traz as 16-20
        doReturn(buildJsonArray(1, 15))
                .doReturn(buildJsonArray(16, 20))
                .when(service).callClaudeRange(anyString(), anyString(), anyInt(), anyInt());

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        verify(service, times(2)).callClaudeRange(anyString(), anyString(), anyInt(), anyInt());
        assertThat(result.questoesSalvas()).isEqualTo(20);
        assertThat(result.partialErrorMessage()).isNull();
    }

    @Test
    @DisplayName("parse registra falha parcial sem abortar job quando retry também falha")
    void parse_falhaParcialNaoAbortaJob() {
        mockCount(40);
        doReturn(buildJsonArray(1, 20))
                .doThrow(new RuntimeException("Anthropic timeout"))
                .doThrow(new RuntimeException("Anthropic timeout"))
                .when(service).callClaudeRange(anyString(), anyString(), anyInt(), anyInt());

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        assertThat(result.questoesSalvas()).isEqualTo(20);
        assertThat(result.partialErrorMessage()).isNotNull().contains("21");
    }

    @Test
    @DisplayName("parse não salva questão já existente com mesmo provaId e enunciado")
    void parse_idempotencia_naoSalvaQuestaoJaExistente() {
        mockCount(1);
        mockRange("[{\"numero\":1,\"enunciado\":\"Q existente\",\"alternativas\":[\"A\",\"B\"],\"gabarito\":\"A\",\"area\":\"TI\",\"dificuldade\":\"FACIL\"}]");
        when(questionRepository.existsByProvaIdAndEnunciado(PROVA_ID, "Q existente")).thenReturn(true);

        var result = service.parse("texto prova", "texto gabarito", BANCA, ANO, CARGO, PROVA_ID);

        verify(questionRepository, never()).save(any());
        assertThat(result.questoesSalvas()).isEqualTo(0);
    }
}
