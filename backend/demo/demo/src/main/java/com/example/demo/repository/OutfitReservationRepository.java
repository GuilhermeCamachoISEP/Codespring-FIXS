package com.example.demo.repository;

import com.example.demo.domain.OutfitReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface OutfitReservationRepository extends JpaRepository<OutfitReservation, Long> {
    List<OutfitReservation> findByUserIdAndEventDate(Long userId, LocalDate eventDate);
}
