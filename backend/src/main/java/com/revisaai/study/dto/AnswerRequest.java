package com.revisaai.study.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerRequest(
        @NotBlank(message = "questionId é obrigatório")
        String questionId,

        @NotBlank(message = "resposta é obrigatória")
        String resposta
) {
}
