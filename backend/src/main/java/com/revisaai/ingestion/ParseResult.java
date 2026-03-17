package com.revisaai.ingestion;

public record ParseResult(int questoesSalvas, int questoesInvalidas, String partialErrorMessage) {
    public ParseResult(int questoesSalvas, int questoesInvalidas) {
        this(questoesSalvas, questoesInvalidas, null);
    }
}
