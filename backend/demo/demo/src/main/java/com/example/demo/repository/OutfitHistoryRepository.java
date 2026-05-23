package com.example.demo.repository;

import com.example.demo.domain.OutfitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitHistoryRepository extends JpaRepository<OutfitHistory, Long> {
    
    List<OutfitHistory> findByUserIdOrderByWornAtDesc(Long userId);
    
    List<OutfitHistory> findTop7ByUserIdOrderByWornAtDesc(Long userId);
    
    List<OutfitHistory> findByUserIdAndIsLikedTrueOrderByWornAtDesc(Long userId);
}
