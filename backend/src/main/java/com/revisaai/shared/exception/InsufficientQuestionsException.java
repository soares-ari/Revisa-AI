package com.revisaai.shared.exception;

public class InsufficientQuestionsException extends RuntimeException {
    public InsufficientQuestionsException(int solicitadas, int disponiveis) {
        super("Questões insuficientes: solicitadas=" + solicitadas +
              ", disponíveis=" + disponiveis);
    }
}
