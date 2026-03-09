package com.revisaai.ingestion;

import com.revisaai.question.Banca;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
@DisplayName("ProvaRepository")
class ProvaRepositoryTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    ProvaRepository repository;

    @Test
    @DisplayName("save e findById preserva todos os campos")
    void saveAndRetrieve_preservaAllFields() {
        var prova = new Prova(Banca.CEBRASPE, 2024, "PF", "Analista", "prova2024.pdf");
        var saved = repository.save(prova);

        var found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getBanca()).isEqualTo(Banca.CEBRASPE);
        assertThat(found.getAno()).isEqualTo(2024);
        assertThat(found.getOrgao()).isEqualTo("PF");
        assertThat(found.getCargo()).isEqualTo("Analista");
        assertThat(found.getFileName()).isEqualTo("prova2024.pdf");
        assertThat(found.getId()).isNotBlank();
    }
}
