package com.revisaai.shared.exception;

public class StudySessionNotFoundException extends RuntimeException {
    public StudySessionNotFoundException(String id) {
        super("Sessão de estudo não encontrada: " + id);
    }
}
