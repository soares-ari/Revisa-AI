package com.revisaai.user;

import com.revisaai.study.Answer;
import com.revisaai.study.SessionStatus;
import com.revisaai.study.StudySession;
import com.revisaai.study.StudySessionRepository;
import com.revisaai.user.dto.SessionSummary;
import com.revisaai.user.dto.UserStatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final StudySessionRepository sessionRepository;

    public UserService(StudySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public UserStatsResponse getStats(String userId) {
        List<StudySession> sessions =
                sessionRepository.findByUserIdAndStatus(userId, SessionStatus.FINALIZADA);

        int totalSessoes = sessions.size();

        int totalQuestoes = sessions.stream()
                .mapToInt(s -> s.getAnswers().size())
                .sum();

        long totalAcertos = sessions.stream()
                .flatMap(s -> s.getAnswers().stream())
                .filter(Answer::isCorreta)
                .count();

        double percentualAcertos = totalQuestoes > 0
                ? (totalAcertos * 100.0) / totalQuestoes
                : 0.0;

        Map<String, Double> desempenhoPorArea = sessions.stream()
                .filter(s -> s.getResultado() != null
                        && s.getResultado().getDesempenhoPorArea() != null)
                .flatMap(s -> s.getResultado().getDesempenhoPorArea().entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.averagingDouble(Map.Entry::getValue)
                ));

        log.debug("Stats userId={}: sessoes={}, questoes={}, acertos%={}",
                userId, totalSessoes, totalQuestoes, percentualAcertos);

        return new UserStatsResponse(totalQuestoes, percentualAcertos,
                totalSessoes, desempenhoPorArea);
    }

    public List<SessionSummary> getHistory(String userId) {
        return sessionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, SessionStatus.FINALIZADA)
                .stream()
                .map(s -> new SessionSummary(
                        s.getId(),
                        s.getBanca(),
                        s.getAreas(),
                        s.getQuantidade(),
                        s.getModo(),
                        s.getResultado(),
                        s.getCreatedAt()
                ))
                .toList();
    }
}
