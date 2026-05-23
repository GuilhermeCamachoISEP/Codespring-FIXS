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

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:file:./data/fashiondb",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
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
        
        int updatedBottoms = jdbc.update("UPDATE wardrobe_items SET category='bottoms' WHERE subcategory LIKE '%calças%' OR subcategory LIKE '%calções%'");
        int updatedShoes = jdbc.update("UPDATE wardrobe_items SET category='shoes' WHERE subcategory LIKE '%Vans%' OR subcategory LIKE '%Air Force%' OR subcategory LIKE '%ténis%'");
        System.out.println("Updated bottoms: " + updatedBottoms);
        System.out.println("Updated shoes: " + updatedShoes);
        List<java.util.Map<String, Object>> items = jdbc.queryForList("SELECT * FROM wardrobe_items WHERE user_id=2");
        System.out.println("Total items for user 2: " + items.size());
        for (java.util.Map<String, Object> i : items) {
            System.out.println("Item: " + i);
        }
        System.out.println("====================================================");
    }
}
