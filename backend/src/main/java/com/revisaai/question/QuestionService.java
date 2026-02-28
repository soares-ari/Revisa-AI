package com.revisaai.question;

import com.revisaai.shared.exception.QuestionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final MongoTemplate mongoTemplate;
    private final QuestionRepository questionRepository;

    public QuestionService(MongoTemplate mongoTemplate, QuestionRepository questionRepository) {
        this.mongoTemplate = mongoTemplate;
        this.questionRepository = questionRepository;
    }

    public List<Question> findAll(String banca, String area, Integer ano) {
        var query = new Query();

        if (banca != null && !banca.isBlank()) {
            query.addCriteria(Criteria.where("banca").is(Banca.valueOf(banca.toUpperCase())));
        }
        if (area != null && !area.isBlank()) {
            query.addCriteria(Criteria.where("area").regex(area, "i"));
        }
        if (ano != null) {
            query.addCriteria(Criteria.where("ano").is(ano));
        }

        log.debug("Consultando questões — banca={}, area={}, ano={}", banca, area, ano);
        return mongoTemplate.find(query, Question.class);
    }

    public Question findById(String id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Questão não encontrada: {}", id);
                    return new QuestionNotFoundException(id);
                });
    }
}
