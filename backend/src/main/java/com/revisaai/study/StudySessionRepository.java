package com.revisaai.study;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface StudySessionRepository extends MongoRepository<StudySession, String> {
}
