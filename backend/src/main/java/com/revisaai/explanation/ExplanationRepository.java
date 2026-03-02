package com.revisaai.explanation;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ExplanationRepository extends MongoRepository<Explanation, String> {
    Optional<Explanation> findByQuestionId(String questionId);
}
