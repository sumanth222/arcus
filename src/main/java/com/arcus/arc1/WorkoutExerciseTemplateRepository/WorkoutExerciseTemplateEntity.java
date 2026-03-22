package com.arcus.arc1.WorkoutExerciseTemplateRepository;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "workout_exercise_template", schema = "public")
public class WorkoutExerciseTemplateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "day_number")
    private Integer dayNumber;

    @Column(name = "exercise_library_id")
    private Integer exerciseLibraryId;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }

    public WorkoutExerciseTemplateEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public Integer getExerciseLibraryId() {
        return exerciseLibraryId;
    }

    public void setExerciseLibraryId(Integer exerciseLibraryId) {
        this.exerciseLibraryId = exerciseLibraryId;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

