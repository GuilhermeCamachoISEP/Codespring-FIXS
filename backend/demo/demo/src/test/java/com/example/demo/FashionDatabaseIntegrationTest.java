package com.example.demo;

import com.example.demo.domain.User;
import com.example.demo.domain.UserPreferences;
import com.example.demo.domain.SwipeResult;
import com.example.demo.domain.WardrobeItem;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserPreferencesRepository;
import com.example.demo.repository.SwipeResultRepository;
import com.example.demo.repository.WardrobeItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FashionDatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private SwipeResultRepository swipeResultRepository;

    @Autowired
    private WardrobeItemRepository wardrobeItemRepository;

    @Test
    void testUserPersistence() {
        // 1. Create and Save User
        User user = User.builder()
                .email("test-h2@example.com")
                .password("hashed_secure_password")
                .name("Hackathon Tester")
                .build();

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        // 2. Retrieve User
        Optional<User> foundUser = userRepository.findByEmail("test-h2@example.com");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("Hackathon Tester");

        // 3. Verify existsByEmail
        boolean exists = userRepository.existsByEmail("test-h2@example.com");
        assertThat(exists).isTrue();
    }

    @Test
    void testUserPreferencesPersistence() {
        // 1. Save User first to get a valid userId
        User user = userRepository.save(User.builder()
                .email("prefs-test@example.com")
                .password("pass")
                .name("Pref User")
                .build());

        Long userId = user.getId();

        // 2. Create and Save Preferences
        UserPreferences preferences = UserPreferences.builder()
                .userId(userId)
                .styleWeights("{\"streetwear\":0.8,\"minimalist\":0.9}")
                .gender("unisex")
                .ageRange("18-25")
                .budgetRange("medium")
                .build();

        UserPreferences savedPrefs = userPreferencesRepository.save(preferences);
        assertThat(savedPrefs.getId()).isNotNull();

        // 3. Retrieve Preferences
        Optional<UserPreferences> foundPrefs = userPreferencesRepository.findByUserId(userId);
        assertThat(foundPrefs).isPresent();
        assertThat(foundPrefs.get().getGender()).isEqualTo("unisex");
        assertThat(foundPrefs.get().getStyleWeights()).contains("streetwear");
    }

    @Test
    void testSwipeResultPersistence() {
        // 1. Save User
        User user = userRepository.save(User.builder()
                .email("swipe-test@example.com")
                .password("pass")
                .name("Swipe User")
                .build());

        Long userId = user.getId();

        // 2. Save Swipe Results
        SwipeResult swipe1 = SwipeResult.builder()
                .userId(userId)
                .imageId("style_img_1")
                .liked(true)
                .build();

        SwipeResult swipe2 = SwipeResult.builder()
                .userId(userId)
                .imageId("style_img_2")
                .liked(false)
                .build();

        swipeResultRepository.save(swipe1);
        swipeResultRepository.save(swipe2);

        // 3. Retrieve Swipes
        List<SwipeResult> userSwipes = swipeResultRepository.findByUserId(userId);
        assertThat(userSwipes).hasSize(2);
        assertThat(userSwipes).extracting(SwipeResult::getImageId)
                .containsExactlyInAnyOrder("style_img_1", "style_img_2");
    }

    @Test
    void testWardrobeItemPersistence() {
        // 1. Save User
        User user = userRepository.save(User.builder()
                .email("wardrobe-test@example.com")
                .password("pass")
                .name("Wardrobe User")
                .build());

        Long userId = user.getId();

        // 2. Save Wardrobe Item
        WardrobeItem item = WardrobeItem.builder()
                .userId(userId)
                .imageUrl("/uploads/test_hoodie.jpg")
                .category("tops")
                .subcategory("hoodie")
                .color("black")
                .fit("oversized")
                .material("cotton")
                .brand("BrandName")
                .season("[\"fall\",\"winter\"]")
                .styleTags("[\"streetwear\",\"casual\"]")
                .timesUsed(0)
                .favoriteScore(0.0)
                .build();

        WardrobeItem savedItem = wardrobeItemRepository.save(item);
        assertThat(savedItem.getId()).isNotNull();
        assertThat(savedItem.getCreatedAt()).isNotNull();

        // 3. Retrieve Wardrobe Items
        List<WardrobeItem> wardrobe = wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(wardrobe).hasSize(1);
        assertThat(wardrobe.get(0).getSubcategory()).isEqualTo("hoodie");
        assertThat(wardrobe.get(0).getBrand()).isEqualTo("BrandName");

        // 4. Retrieve by Category
        List<WardrobeItem> tops = wardrobeItemRepository.findByUserIdAndCategory(userId, "tops");
        assertThat(tops).hasSize(1);

        List<WardrobeItem> bottoms = wardrobeItemRepository.findByUserIdAndCategory(userId, "bottoms");
        assertThat(bottoms).isEmpty();
    }
}
