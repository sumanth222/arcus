package com.arcus.arc1.Nutrition;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Lightweight insight/tip generated for the user about their nutrition habits.
 * No strict tracking — heuristic-based messages.
 *
 * type values: INFO | WARNING | SUCCESS
 */
@Entity
@Table(name = "nutrition_insight")
public class NutritionInsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Human-readable message shown in the app */
    @Column(nullable = false, length = 500)
    private String message;

    /** INFO | WARNING | SUCCESS */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public NutritionInsightEntity() {}

    public NutritionInsightEntity(Long userId, String message, String type) {
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

