package com.revisaai.study.dto;

public record AnswerResponse(
        String questionId,
        String respostaUsuario,
        boolean correta,
        String area,
        String gabarito
) {
}
