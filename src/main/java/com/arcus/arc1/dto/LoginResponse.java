package com.arcus.arc1.dto;

public class LoginResponse {

    private Long userId;
    private Long credentialsId;
    private String name;
    private boolean newUser;
    private String username;

    public LoginResponse() {}

    public LoginResponse(Long userId, Long credentialsId, String name, boolean newUser) {
        this.userId = userId;
        this.credentialsId = credentialsId;
        this.name = name;
        this.newUser = newUser;
    }

    public LoginResponse(Long userId, Long credentialsId, String name, boolean newUser, String username) {
        this.userId = userId;
        this.credentialsId = credentialsId;
        this.name = name;
        this.newUser = newUser;
        this.username = username;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCredentialsId() { return credentialsId; }
    public void setCredentialsId(Long credentialsId) { this.credentialsId = credentialsId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isNewUser() { return newUser; }
    public void setNewUser(boolean newUser) { this.newUser = newUser; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
