package com.revisaai.explanation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/explanations")
public class ExplanationController {

    private final ExplanationService explanationService;

    public ExplanationController(ExplanationService explanationService) {
        this.explanationService = explanationService;
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<Explanation> getExplanation(
            @PathVariable String questionId,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
