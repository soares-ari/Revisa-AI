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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionParserService {

    static final int BATCH_SIZE = 20;

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
        throw new UnsupportedOperationException("not implemented");
    }

    int callClaudeCount(String textProva) {
        throw new UnsupportedOperationException("not implemented");
    }

    String callClaudeRange(String textProva, String textGabarito, int inicio, int fim) {
        throw new UnsupportedOperationException("not implemented");
    }

    private Dificuldade parseDificuldadeOrNull(String value) {
        try {
            return Dificuldade.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    String callClaude(String prompt) {
        var params = MessageCreateParams.builder()
                .model("claude-haiku-4-5-20251001")
                .maxTokens(8192L)
                .addUserMessage(prompt)
                .build();

        var message = anthropicClient.messages().create(params);

        var rawJson = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining());

        return rawJson.replaceAll("(?s)```json\\s*|```\\s*", "").trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QuestionJson(int numero, String enunciado, List<String> alternativas,
                        String gabarito, String area, String dificuldade) {}
}
