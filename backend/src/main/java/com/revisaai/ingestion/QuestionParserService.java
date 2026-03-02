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
                              Banca banca, Integer ano, String cargo) {
        var prompt = """
                Você receberá o texto extraído de uma prova de concurso público e de seu gabarito.
                Extraia todas as questões e combine com as respostas do gabarito.
                Responda EXCLUSIVAMENTE com um array JSON, sem preâmbulo, explicação ou markdown.
                Cada elemento deve ter: enunciado (string), alternativas (array de strings com o texto
                de cada alternativa), gabarito (alternativa correta exatamente como aparece em alternativas),
                area (string com área de conhecimento), dificuldade (uma de: FACIL, MEDIO, DIFICIL).

                TEXTO DA PROVA:
                %s

                GABARITO:
                %s
                """.formatted(textProva, textGabarito);

        var json = callClaude(prompt);

        List<QuestionJson> items;
        try {
            items = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Falha ao parsear resposta da Anthropic: " + e.getMessage(), e);
        }

        int questoesSalvas = 0;
        int questoesInvalidas = 0;

        for (var item : items) {
            Question question;
            try {
                var dificuldade = Dificuldade.valueOf(item.dificuldade());
                question = new Question(item.enunciado(), item.alternativas(),
                        item.gabarito(), banca, ano, cargo, item.area(), dificuldade);
                questoesSalvas++;
            } catch (IllegalArgumentException e) {
                var dificuldade = parseDificuldadeOrNull(item.dificuldade());
                question = Question.createInvalid(item.enunciado(), item.alternativas(),
                        item.gabarito(), banca, ano, cargo, item.area(), dificuldade,
                        e.getMessage());
                questoesInvalidas++;
            }
            questionRepository.save(question);
        }

        return new ParseResult(questoesSalvas, questoesInvalidas);
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

    private Dificuldade parseDificuldadeOrNull(String value) {
        try {
            return Dificuldade.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuestionJson(String enunciado, List<String> alternativas,
                                 String gabarito, String area, String dificuldade) {}
}
