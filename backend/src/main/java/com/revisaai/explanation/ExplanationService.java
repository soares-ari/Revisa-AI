package com.revisaai.explanation;

import com.anthropic.client.AnthropicClient;
import com.revisaai.question.QuestionRepository;
import org.springframework.stereotype.Service;

@Service
public class ExplanationService {

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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    String callClaude(String prompt) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
