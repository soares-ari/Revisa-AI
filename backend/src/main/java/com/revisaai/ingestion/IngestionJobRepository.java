package com.revisaai.ingestion;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface IngestionJobRepository extends MongoRepository<IngestionJob, String> {
}
