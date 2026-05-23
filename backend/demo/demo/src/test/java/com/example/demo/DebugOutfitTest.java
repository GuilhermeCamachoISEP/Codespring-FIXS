package com.example.demo;

import com.example.demo.dto.OutfitSuggestion;
import com.example.demo.service.OutfitService;
import com.example.demo.repository.WardrobeItemRepository;
import com.example.demo.domain.WardrobeItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@SpringBootTest
@TestPropertySource(properties = { "spring.datasource.url=jdbc:h2:file:../../data/fashiondb" })
public class DebugOutfitTest {

    @Autowired
    private OutfitService outfitService;

    @Autowired
    private WardrobeItemRepository repository;
    
    @Autowired
    private org.springframework.context.ApplicationContext context;

    @Test
    public void debugOutfits() {
        System.out.println("========== DEBUGGING ROOT DB WARDROBE ==========");
        org.springframework.jdbc.core.JdbcTemplate jdbc = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        
        List<java.util.Map<String, Object>> users = jdbc.queryForList("SELECT id, name FROM app_users");
        for (java.util.Map<String, Object> u : users) {
            System.out.println("User: ID=" + u.get("ID") + " Name=" + u.get("NAME"));
        }
        
        List<java.util.Map<String, Object>> res = jdbc.queryForList("SELECT * FROM event_reservation");
        System.out.println("Total reservations: " + res.size());
        for (java.util.Map<String, Object> r : res) {
            System.out.println("Res: " + r);
        }
        System.out.println("====================================================");
    }
}
