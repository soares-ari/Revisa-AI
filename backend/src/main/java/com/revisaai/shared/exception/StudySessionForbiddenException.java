package com.revisaai.shared.exception;

public class StudySessionForbiddenException extends RuntimeException {
    public StudySessionForbiddenException() {
        super("Acesso negado à sessão de estudo");
    }
}
