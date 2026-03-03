package com.revisaai.user.dto;

import com.revisaai.question.Banca;
import com.revisaai.study.Resultado;
import com.revisaai.study.SessionModo;

import java.time.Instant;
import java.util.List;

public record SessionSummary(
        String id,
        Banca banca,
        List<String> areas,
        int quantidade,
        SessionModo modo,
        Resultado resultado,
        Instant createdAt
) {
}
