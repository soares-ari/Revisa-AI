package com.revisaai.user.dto;

import java.util.Map;

public record UserStatsResponse(
        int totalQuestoes,
        double percentualAcertos,
        int totalSessoes,
        Map<String, Double> desempenhoPorArea
) {
}
