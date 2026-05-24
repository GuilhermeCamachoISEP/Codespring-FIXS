package com.example.demo.repository;

import com.example.demo.domain.OutfitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OutfitHistoryRepository extends JpaRepository<OutfitHistory, Long> {

    /** Full history (generated + worn), newest first */
    List<OutfitHistory> findByUserIdOrderByWornAtDesc(Long userId);

    /** Only outfits the user actually wore, newest first */
    List<OutfitHistory> findByUserIdAndWornTrueOrderByWornDateDesc(Long userId);

    /** Last 7 WORN outfits — used for anti-repetition in outfit generation */
    List<OutfitHistory> findTop7ByUserIdAndWornTrueOrderByWornDateDesc(Long userId);

    /** Last 7 generated outfits (legacy fallback if nothing worn yet) */
    List<OutfitHistory> findTop7ByUserIdOrderByWornAtDesc(Long userId);

    List<OutfitHistory> findByUserIdAndIsLikedTrueOrderByWornAtDesc(Long userId);

    /** Worn outfits from the last N days — used to enforce no-repeat rule */
    @Query("SELECT h FROM OutfitHistory h WHERE h.userId = :userId AND h.worn = true AND h.wornDate >= :since ORDER BY h.wornDate DESC")
    List<OutfitHistory> findWornSince(@Param("userId") Long userId, @Param("since") LocalDate since);

    /** Event-reserved outfits (eventName is set), newest first */
    List<OutfitHistory> findByUserIdAndEventNameNotNullOrderByWornAtDesc(Long userId);
}
