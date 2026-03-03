package com.revisaai.user;

import com.revisaai.study.StudySessionRepository;
import com.revisaai.user.dto.SessionSummary;
import com.revisaai.user.dto.UserStatsResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final StudySessionRepository sessionRepository;

    public UserService(StudySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public UserStatsResponse getStats(String userId) {
        throw new UnsupportedOperationException("não implementado");
    }

    public List<SessionSummary> getHistory(String userId) {
        throw new UnsupportedOperationException("não implementado");
    }
}
