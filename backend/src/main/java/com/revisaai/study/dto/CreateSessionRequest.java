package com.revisaai.study.dto;

import com.revisaai.study.SessionModo;

import java.util.List;

public record CreateSessionRequest(
        String banca,
        List<String> areas,
        Integer quantidade,
        SessionModo modo
) {
}
