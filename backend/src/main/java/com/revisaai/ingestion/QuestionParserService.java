package com.revisaai.ingestion;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisaai.question.Banca;
import com.revisaai.question.Dificuldade;
import com.revisaai.question.Question;
import com.revisaai.question.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionParserService {

    static final int BATCH_SIZE = 20;

    private static final Logger log = LoggerFactory.getLogger(QuestionParserService.class);

    private final AnthropicClient anthropicClient;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public QuestionParserService(AnthropicClient anthropicClient,
                                  QuestionRepository questionRepository,
                                  ObjectMapper objectMapper) {
        this.anthropicClient = anthropicClient;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
    }

    public ParseResult parse(String textProva, String textGabarito,
                              Banca banca, Integer ano, String cargo, String provaId) {
        int totalQuestoes = callClaudeCount(textProva);
        if (totalQuestoes <= 0) {
            return new ParseResult(0, 0);
        }

        Set<Integer> covered = new HashSet<>();
        int questoesSalvas = 0;
        int questoesInvalidas = 0;
        List<int[]> failedRanges = new ArrayList<>();

        // Fase 2 — extração por lotes
        for (int inicio = 1; inicio <= totalQuestoes; inicio += BATCH_SIZE) {
            int fim = Math.min(inicio + BATCH_SIZE - 1, totalQuestoes);
            try {
                var json = callClaudeRange(textProva, textGabarito, inicio, fim);
                var saved = persistBatch(json, banca, ano, cargo, provaId, covered);
                questoesSalvas += saved.questoesSalvas();
                questoesInvalidas += saved.questoesInvalidas();
            } catch (Exception e) {
                log.warn("Falha no batch {}-{}: {}", inicio, fim, e.getMessage());
                failedRanges.add(new int[]{inicio, fim});
            }
        }

        // Fase 3 — validação e retry
        Set<Integer> missing = computeMissing(totalQuestoes, covered);
        List<int[]> retryRanges = mergeRanges(missing, failedRanges);

        List<String> errors = new ArrayList<>();
        for (var range : retryRanges) {
            int inicio = range[0];
            int fim = range[1];
            try {
                var json = callClaudeRange(textProva, textGabarito, inicio, fim);
                var saved = persistBatch(json, banca, ano, cargo, provaId, covered);
                questoesSalvas += saved.questoesSalvas();
                questoesInvalidas += saved.questoesInvalidas();
            } catch (Exception e) {
                log.error("Retry falhou para questões {}-{}: {}", inicio, fim, e.getMessage());
                errors.add("Questões %d-%d não foram extraídas.".formatted(inicio, fim));
            }
        }

        String partialError = errors.isEmpty() ? null : String.join(" ", errors);
        return new ParseResult(questoesSalvas, questoesInvalidas, partialError);
    }

    int callClaudeCount(String textProva) {
        var prompt = """
                Quantas questões existem nesta prova de concurso público?
                Responda APENAS com o número inteiro. Não inclua texto, pontuação ou formatação adicional.

                TEXTO DA PROVA:
                %s
                """.formatted(textProva);
        var response = callClaude(prompt).strip();
        return Integer.parseInt(response);
    }

    String callClaudeRange(String textProva, String textGabarito, int inicio, int fim) {
        var prompt = """
                Você receberá o texto extraído de uma prova de concurso público e de seu gabarito.
                Extraia SOMENTE as questões numeradas de %d a %d (inclusive) e combine com o gabarito.
                Responda EXCLUSIVAMENTE com um array JSON, sem preâmbulo, explicação ou markdown.
                Se uma questão desse intervalo não existir no texto, omita-a do array.
                Cada elemento deve ter obrigatoriamente:
                - numero (int — número da questão conforme aparece na prova)
                - enunciado (string)
                - alternativas (array de strings com o texto de cada alternativa)
                - gabarito (string — alternativa correta exatamente como aparece em alternativas)
                - area (string — área de conhecimento)
                - dificuldade (uma de: FACIL, MEDIO, DIFICIL)

                TEXTO DA PROVA:
                %s

                GABARITO:
                %s
                """.formatted(inicio, fim, textProva, textGabarito);
        var raw = callClaude(prompt);
        return raw.replaceAll("(?s)```json\\s*|```\\s*", "").trim();
    }

    private ParseResult persistBatch(String json, Banca banca, Integer ano, String cargo,
                                      String provaId, Set<Integer> covered) {
        List<QuestionJson> items;
        try {
            items = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Falha ao parsear resposta da Anthropic: " + e.getMessage(), e);
        }

        int salvas = 0;
        int invalidas = 0;
        for (var item : items) {
            if (questionRepository.existsByProvaIdAndEnunciado(provaId, item.enunciado())) {
                covered.add(item.numero());
                continue;
            }
            Question question;
            try {
                var dificuldade = Dificuldade.valueOf(item.dificuldade());
                question = new Question(item.enunciado(), item.alternativas(),
                        item.gabarito(), banca, ano, cargo, item.area(), dificuldade, provaId);
                salvas++;
            } catch (IllegalArgumentException e) {
                var dificuldade = parseDificuldadeOrNull(item.dificuldade());
                question = Question.createInvalid(item.enunciado(), item.alternativas(),
                        item.gabarito(), banca, ano, cargo, item.area(), dificuldade,
                        e.getMessage(), provaId);
                invalidas++;
            }
            questionRepository.save(question);
            covered.add(item.numero());
        }
        return new ParseResult(salvas, invalidas);
    }

    private Set<Integer> computeMissing(int total, Set<Integer> covered) {
        Set<Integer> missing = new LinkedHashSet<>();
        for (int i = 1; i <= total; i++) {
            if (!covered.contains(i)) missing.add(i);
        }
        return missing;
    }

    private List<int[]> mergeRanges(Set<Integer> missing, List<int[]> failedRanges) {
        // Combina os números faltantes em ranges contíguos e adiciona os ranges já falhos
        Set<Integer> all = new LinkedHashSet<>(missing);
        for (var r : failedRanges) {
            for (int i = r[0]; i <= r[1]; i++) all.add(i);
        }
        if (all.isEmpty()) return List.of();

        List<int[]> ranges = new ArrayList<>();
        int[] sorted = all.stream().mapToInt(Integer::intValue).sorted().toArray();
        int start = sorted[0];
        int prev = sorted[0];
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] != prev + 1) {
                ranges.add(new int[]{start, prev});
                start = sorted[i];
            }
            prev = sorted[i];
        }
        ranges.add(new int[]{start, prev});
        return ranges;
    }

    private String callClaude(String prompt) {
        var params = MessageCreateParams.builder()
                .model("claude-haiku-4-5-20251001")
                .maxTokens(8192L)
                .addUserMessage(prompt)
                .build();

        var message = anthropicClient.messages().create(params);

        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining());
    }

    private Dificuldade parseDificuldadeOrNull(String value) {
        try {
            return Dificuldade.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QuestionJson(int numero, String enunciado, List<String> alternativas,
                        String gabarito, String area, String dificuldade) {}
}
