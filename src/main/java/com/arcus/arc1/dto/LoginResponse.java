package com.arcus.arc1.dto;

public class LoginResponse {

    private Long userId;
    private String name;
    private boolean newUser;

    public LoginResponse() {}

    public LoginResponse(Long userId, String name, boolean newUser) {
        this.userId = userId;
        this.name = name;
        this.newUser = newUser;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isNewUser() { return newUser; }
    public void setNewUser(boolean newUser) { this.newUser = newUser; }
}

