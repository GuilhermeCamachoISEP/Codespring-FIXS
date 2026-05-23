package com.example.demo.repository;

import com.example.demo.domain.SwipeResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SwipeResultRepository extends JpaRepository<SwipeResult, Long> {
    List<SwipeResult> findByUserId(Long userId);
}
