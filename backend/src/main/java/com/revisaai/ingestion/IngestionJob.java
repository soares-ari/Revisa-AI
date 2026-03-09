package com.revisaai.ingestion;

import com.revisaai.question.Banca;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "ingestion_jobs")
public class IngestionJob {

    @Id
    private String id;

    private Banca banca;
    private Integer ano;
    private String cargo;

    private String textProva;
    private String textGabarito;

    private IngestionStatus status;
    private String errorMessage;

    private int questoesSalvas;
    private int questoesInvalidas;

    private String provaId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public IngestionJob() {}

    public IngestionJob(Banca banca, Integer ano, String cargo) {
        this.banca = banca;
        this.ano = ano;
        this.cargo = cargo;
        this.status = IngestionStatus.PENDING;
    }

    public String getId() { return id; }
    public Banca getBanca() { return banca; }
    public Integer getAno() { return ano; }
    public String getCargo() { return cargo; }
    public String getTextProva() { return textProva; }
    public String getTextGabarito() { return textGabarito; }
    public IngestionStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public int getQuestoesSalvas() { return questoesSalvas; }
    public int getQuestoesInvalidas() { return questoesInvalidas; }

    public void setStatus(IngestionStatus status) { this.status = status; }
    public void setTextProva(String textProva) { this.textProva = textProva; }
    public void setTextGabarito(String textGabarito) { this.textGabarito = textGabarito; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setQuestoesSalvas(int questoesSalvas) { this.questoesSalvas = questoesSalvas; }
    public void setQuestoesInvalidas(int questoesInvalidas) { this.questoesInvalidas = questoesInvalidas; }
    public String getProvaId() { return provaId; }
    public void setProvaId(String provaId) { this.provaId = provaId; }
}
