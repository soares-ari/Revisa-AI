package com.revisaai.question;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "questions")
public class Question {

    @Id
    private String id;

    private String enunciado;
    private List<String> alternativas;
    private String gabarito;

    @Indexed
    private Banca banca;

    private Integer ano;
    private String cargo;

    @Indexed
    private String area;

    private Dificuldade dificuldade;

    private boolean valid = true;
    private String validationError;

    private String provaId;

    @CreatedDate
    private Instant createdAt;

    public Question() {
    }

    public Question(String enunciado, List<String> alternativas, String gabarito,
                    Banca banca, Integer ano, String cargo, String area, Dificuldade dificuldade,
                    String provaId) {
        if (!alternativas.contains(gabarito)) {
            throw new IllegalArgumentException(
                    "Gabarito '" + gabarito + "' não está nas alternativas: " + alternativas);
        }
        this.enunciado = enunciado;
        this.alternativas = alternativas;
        this.gabarito = gabarito;
        this.banca = banca;
        this.ano = ano;
        this.cargo = cargo;
        this.area = area;
        this.dificuldade = dificuldade;
        this.provaId = provaId;
    }

    public static Question createInvalid(String enunciado, List<String> alternativas,
                                          String gabarito, Banca banca, Integer ano,
                                          String cargo, String area, Dificuldade dificuldade,
                                          String validationError, String provaId) {
        var q = new Question();
        q.enunciado = enunciado;
        q.alternativas = alternativas;
        q.gabarito = gabarito;
        q.banca = banca;
        q.ano = ano;
        q.cargo = cargo;
        q.area = area;
        q.dificuldade = dificuldade;
        q.valid = false;
        q.validationError = validationError;
        q.provaId = provaId;
        return q;
    }

    public String getId() { return id; }
    public String getEnunciado() { return enunciado; }
    public List<String> getAlternativas() { return alternativas; }
    public String getGabarito() { return gabarito; }
    public Banca getBanca() { return banca; }
    public Integer getAno() { return ano; }
    public String getCargo() { return cargo; }
    public String getArea() { return area; }
    public Dificuldade getDificuldade() { return dificuldade; }
    public boolean isValid() { return valid; }
    public String getValidationError() { return validationError; }
    public String getProvaId() { return provaId; }
    public Instant getCreatedAt() { return createdAt; }
}
