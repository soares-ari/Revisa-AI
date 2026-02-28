package com.revisaai.shared.exception;

public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(String id) {
        super("Questão não encontrada: " + id);
    }
}
