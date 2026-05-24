package com.example.demo.repository;

import com.example.demo.domain.WardrobeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WardrobeItemRepository extends JpaRepository<WardrobeItem, Long> {
    List<WardrobeItem> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WardrobeItem> findByUserIdAndCategory(Long userId, String category);
    long countByUserId(Long userId);
    List<WardrobeItem> findByUserIdAndSubcategoryIgnoreCaseOrderByCreatedAtDesc(Long userId, String subcategory);

    /** Used to detect duplicate chat-additions within a short window */
    @Query("SELECT COUNT(w) > 0 FROM WardrobeItem w WHERE w.userId = :userId " +
           "AND LOWER(w.subcategory) = LOWER(:subcategory) AND w.category = :category " +
           "AND w.createdAt >= :since")
    boolean existsRecentDuplicate(@Param("userId") Long userId,
                                  @Param("subcategory") String subcategory,
                                  @Param("category") String category,
                                  @Param("since") LocalDateTime since);
}
