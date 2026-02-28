package com.revisaai.auth.oauth2;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AuthCodeRepository extends MongoRepository<AuthCode, String> {

    Optional<AuthCode> findByCode(String code);

    void deleteByCode(String code);
}
