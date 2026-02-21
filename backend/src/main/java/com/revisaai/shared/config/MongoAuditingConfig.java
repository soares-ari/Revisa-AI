package com.revisaai.shared.config;

import com.mongodb.client.MongoClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@ConditionalOnBean(MongoClient.class)
@EnableMongoAuditing
public class MongoAuditingConfig {
}
