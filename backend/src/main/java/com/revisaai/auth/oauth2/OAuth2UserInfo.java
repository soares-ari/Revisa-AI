package com.revisaai.auth.oauth2;

public interface OAuth2UserInfo {

    String getId();

    String getName();

    String getEmail();

    String getPictureUrl();
}
