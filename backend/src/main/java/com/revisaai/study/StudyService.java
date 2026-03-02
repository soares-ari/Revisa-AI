package com.revisaai.study;

import com.revisaai.question.Banca;
import com.revisaai.question.Question;
import com.revisaai.question.QuestionRepository;
import com.revisaai.shared.exception.InsufficientQuestionsException;
import com.revisaai.shared.exception.StudySessionForbiddenException;
import com.revisaai.shared.exception.StudySessionNotFoundException;
import com.revisaai.study.dto.AnswerRequest;
import com.revisaai.study.dto.AnswerResponse;
import com.revisaai.study.dto.CreateSessionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class StudyService {

    private static final Logger log = LoggerFactory.getLogger(StudyService.class);

    private final MongoTemplate mongoTemplate;
    private final StudySessionRepository sessionRepository;
    private final QuestionRepository questionRepository;

    public StudyService(MongoTemplate mongoTemplate,
                        StudySessionRepository sessionRepository,
                        QuestionRepository questionRepository) {
        this.mongoTemplate = mongoTemplate;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
    }

    public StudySession create(CreateSessionRequest request, String userId) {
        int qtd = request.quantidade() != null ? request.quantidade() : 20;

        var criteria = Criteria.where("valid").is(true);
        if (request.banca() != null && !request.banca().isBlank()) {
            criteria = criteria.and("banca").is(Banca.valueOf(request.banca().toUpperCase()));
        }
        if (request.areas() != null && !request.areas().isEmpty()) {
            criteria = criteria.and("area").in(request.areas());
        }

        var aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sample(qtd));

        var questions = mongoTemplate.aggregate(aggregation, "questions", Question.class)
                .getMappedResults();

        log.debug("Seleção de questões: solicitadas={}, encontradas={}", qtd, questions.size());

        if (questions.size() < qtd) {
            throw new InsufficientQuestionsException(qtd, questions.size());
        }

        var questionIds = questions.stream().map(Question::getId).toList();
        var modo = request.modo() != null ? request.modo() : SessionModo.ESTUDO;
        Banca bancaEnum = request.banca() != null && !request.banca().isBlank()
                ? Banca.valueOf(request.banca().toUpperCase()) : null;

        var session = new StudySession(userId, bancaEnum, request.areas(), qtd, modo, questionIds);
        return sessionRepository.save(session);
    }

    public StudySession findById(String id, String userId) {
        var session = sessionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Sessão não encontrada: {}", id);
                    return new StudySessionNotFoundException(id);
                });
        if (!session.getUserId().equals(userId)) {
            throw new StudySessionForbiddenException();
        }
        return session;
    }

    public AnswerResponse answer(String sessionId, AnswerRequest request, String userId) {
        var session = findById(sessionId, userId);

        if (session.getStatus() == SessionStatus.FINALIZADA) {
            throw new IllegalArgumentException("Sessão já está FINALIZADA");
        }
        if (!session.getQuestionIds().contains(request.questionId())) {
            throw new IllegalArgumentException(
                    "Questão '" + request.questionId() + "' não pertence a esta sessão");
        }
        boolean jaRespondida = session.getAnswers().stream()
                .anyMatch(a -> a.getQuestionId().equals(request.questionId()));
        if (jaRespondida) {
            throw new IllegalArgumentException(
                    "Questão '" + request.questionId() + "' já respondida nesta sessão");
        }

        var question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Questão não encontrada: " + request.questionId()));

        boolean correta = question.getGabarito().equals(request.resposta());
        var answer = new Answer(request.questionId(), request.resposta(), correta, question.getArea());

        session.getAnswers().add(answer);
        session.setCurrentIndex(session.getCurrentIndex() + 1);
        sessionRepository.save(session);

        String gabarito = session.getModo() == SessionModo.ESTUDO ? question.getGabarito() : null;
        return new AnswerResponse(
                answer.getQuestionId(), answer.getRespostaUsuario(),
                answer.isCorreta(), answer.getArea(), gabarito);
    }

    public StudySession finish(String sessionId, String userId) {
        var session = findById(sessionId, userId);

        int total = session.getAnswers().size();
        int acertos = (int) session.getAnswers().stream().filter(Answer::isCorreta).count();
        double percentual = total > 0 ? acertos * 100.0 / total : 0.0;

        var byArea = session.getAnswers().stream()
                .collect(Collectors.groupingBy(Answer::getArea));

        var desempenhoPorArea = new HashMap<String, Double>();
        byArea.forEach((area, answers) -> {
            long areaAcertos = answers.stream().filter(Answer::isCorreta).count();
            desempenhoPorArea.put(area, areaAcertos * 100.0 / answers.size());
        });

        session.setResultado(new Resultado(total, acertos, percentual, desempenhoPorArea));
        session.setStatus(SessionStatus.FINALIZADA);

        log.debug("Sessão {} finalizada: {}/{}", sessionId, acertos, total);

        return sessionRepository.save(session);
    }
}
