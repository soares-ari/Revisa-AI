package com.revisaai.ingestion;

import com.revisaai.question.Banca;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "provas")
public class Prova {

    @Id
    private String id;

    private Banca banca;
    private int ano;
    private String orgao;
    private String cargo;
    private String fileName;

    @CreatedDate
    private Instant createdAt;

    public Prova() {}

    public Prova(Banca banca, int ano, String orgao, String cargo, String fileName) {
        this.banca = banca;
        this.ano = ano;
        this.orgao = orgao;
        this.cargo = cargo;
        this.fileName = fileName;
    }

    public String getId() { return id; }
    public Banca getBanca() { return banca; }
    public int getAno() { return ano; }
    public String getOrgao() { return orgao; }
    public String getCargo() { return cargo; }
    public String getFileName() { return fileName; }
    public Instant getCreatedAt() { return createdAt; }
}
