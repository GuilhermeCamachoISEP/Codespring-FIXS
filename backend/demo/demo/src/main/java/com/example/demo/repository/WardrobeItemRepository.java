package com.example.demo.repository;

import com.example.demo.domain.WardrobeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WardrobeItemRepository extends JpaRepository<WardrobeItem, Long> {
    List<WardrobeItem> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WardrobeItem> findByUserIdAndCategory(Long userId, String category);
    long countByUserId(Long userId);
    List<WardrobeItem> findByUserIdAndSubcategoryIgnoreCaseOrderByCreatedAtDesc(Long userId, String subcategory);
}
