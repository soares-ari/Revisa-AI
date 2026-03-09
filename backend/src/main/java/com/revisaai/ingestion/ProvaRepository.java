package com.revisaai.ingestion;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProvaRepository extends MongoRepository<Prova, String> {
}
