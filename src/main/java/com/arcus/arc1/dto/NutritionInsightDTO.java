package com.arcus.arc1.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for a single nutrition insight entry.
 */
public class NutritionInsightDTO {

    private Long id;
    private String message;
    /** INFO | WARNING | SUCCESS */
    private String type;
    private LocalDateTime createdAt;

    public NutritionInsightDTO() {}

    public NutritionInsightDTO(Long id, String message, String type, LocalDateTime createdAt) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

