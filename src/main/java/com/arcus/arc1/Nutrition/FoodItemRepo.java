package com.arcus.arc1.Nutrition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepo extends JpaRepository<FoodItemEntity, Long> {
    boolean existsByName(String name);
    Optional<FoodItemEntity> findByName(String name);
    List<FoodItemEntity> findByCategory(String category);
}

