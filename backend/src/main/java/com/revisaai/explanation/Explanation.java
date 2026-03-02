package com.revisaai.explanation;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "explanations")
public class Explanation {

    @Id
    private String id;

    @Indexed(unique = true)
    private String questionId;

    private String texto;

    @CreatedDate
    private Instant createdAt;

    public Explanation() {
    }

    public Explanation(String questionId, String texto) {
        this.questionId = questionId;
        this.texto = texto;
    }

    public String getId() { return id; }
    public String getQuestionId() { return questionId; }
    public String getTexto() { return texto; }
    public Instant getCreatedAt() { return createdAt; }
}
