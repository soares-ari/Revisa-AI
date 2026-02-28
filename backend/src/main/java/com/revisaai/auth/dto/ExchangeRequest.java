package com.revisaai.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeRequest(@NotBlank String code) {
}
