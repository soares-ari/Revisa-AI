package com.revisaai.auth.oauth2;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "auth_codes")
public class AuthCode {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private String userId;

    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

    public AuthCode() {}

    public AuthCode(String code, String userId, Instant expiresAt) {
        this.code = code;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
}
