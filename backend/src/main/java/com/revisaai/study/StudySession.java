package com.revisaai.study;

import com.revisaai.question.Banca;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "study_sessions")
public class StudySession {

    @Id
    private String id;

    private String userId;
    private Banca banca;
    private List<String> areas;
    private int quantidade;
    private SessionModo modo;
    private SessionStatus status = SessionStatus.EM_ANDAMENTO;
    private List<String> questionIds;
    private int currentIndex = 0;
    private List<Answer> answers = new ArrayList<>();
    private Resultado resultado;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public StudySession() {
    }

    public StudySession(String userId, Banca banca, List<String> areas,
                        int quantidade, SessionModo modo, List<String> questionIds) {
        this.userId = userId;
        this.banca = banca;
        this.areas = areas;
        this.quantidade = quantidade;
        this.modo = modo;
        this.questionIds = questionIds;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public Banca getBanca() { return banca; }
    public List<String> getAreas() { return areas; }
    public int getQuantidade() { return quantidade; }
    public SessionModo getModo() { return modo; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public List<String> getQuestionIds() { return questionIds; }
    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }
    public List<Answer> getAnswers() { return answers; }
    public Resultado getResultado() { return resultado; }
    public void setResultado(Resultado resultado) { this.resultado = resultado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
