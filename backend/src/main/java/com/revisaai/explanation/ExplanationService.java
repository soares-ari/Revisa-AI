package com.revisaai.explanation;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.revisaai.question.Question;
import com.revisaai.question.QuestionRepository;
import com.revisaai.shared.exception.QuestionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ExplanationService.class);

    private final AnthropicClient anthropicClient;
    private final QuestionRepository questionRepository;
    private final ExplanationRepository explanationRepository;

    public ExplanationService(AnthropicClient anthropicClient,
                               QuestionRepository questionRepository,
                               ExplanationRepository explanationRepository) {
        this.anthropicClient = anthropicClient;
        this.questionRepository = questionRepository;
        this.explanationRepository = explanationRepository;
    }

    public Explanation getExplanation(String questionId) {
        var cached = explanationRepository.findByQuestionId(questionId);
        if (cached.isPresent()) {
            log.debug("Cache hit para questionId={}", questionId);
            return cached.get();
        }

        var question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        log.debug("Gerando explicação para questionId={}", questionId);
        var texto = callClaude(buildPrompt(question));

        return explanationRepository.save(new Explanation(questionId, texto));
    }

    String callClaude(String prompt) {
        var params = MessageCreateParams.builder()
                .model("claude-haiku-4-5-20251001")
                .maxTokens(4096L)
                .addUserMessage(prompt)
                .build();

        var message = anthropicClient.messages().create(params);

        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining());
    }

    private String buildPrompt(Question question) {
        return """
                Você é um professor especializado em concursos públicos.
                Explique a questão abaixo em português, sem usar markdown.
                Inclua: justificativa geral da questão, por que o gabarito está correto,
                e por que cada alternativa incorreta está errada.

                Enunciado: %s
                Alternativas: %s
                Gabarito correto: %s
                """.formatted(
                question.getEnunciado(),
                String.join(", ", question.getAlternativas()),
                question.getGabarito());
    }
}
