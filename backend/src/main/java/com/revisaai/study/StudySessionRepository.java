package com.revisaai.study;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StudySessionRepository extends MongoRepository<StudySession, String> {

    List<StudySession> findByUserIdAndStatus(String userId, SessionStatus status);

    List<StudySession> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, SessionStatus status);
}
