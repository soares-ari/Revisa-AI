package com.revisaai.user;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String name;

    private AuthProvider provider;

    private String providerId;

    private String pictureUrl;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public User() {}

    public User(String email, String password, String name, AuthProvider provider) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.provider = provider;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public AuthProvider getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public String getPictureUrl() { return pictureUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setProviderId(String providerId) { this.providerId = providerId; }
    public void setName(String name) { this.name = name; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }
}
