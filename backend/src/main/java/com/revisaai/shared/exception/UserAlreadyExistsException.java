package com.revisaai.shared.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("Usuário já cadastrado com o e-mail: " + email);
    }
}
