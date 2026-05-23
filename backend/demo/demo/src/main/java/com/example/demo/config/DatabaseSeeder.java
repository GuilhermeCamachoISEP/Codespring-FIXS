package com.example.demo.config;

import com.example.demo.domain.SwipeResult;
import com.example.demo.domain.User;
import com.example.demo.domain.UserPreferences;
import com.example.demo.domain.WardrobeItem;
import com.example.demo.repository.SwipeResultRepository;
import com.example.demo.repository.UserPreferencesRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WardrobeItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!prod")
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final WardrobeItemRepository wardrobeItemRepository;
    private final SwipeResultRepository swipeResultRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            UserRepository userRepository,
            UserPreferencesRepository userPreferencesRepository,
            WardrobeItemRepository wardrobeItemRepository,
            SwipeResultRepository swipeResultRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.swipeResultRepository = swipeResultRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Idempotência por bloco: se o utilizador demo já existe, não fazemos nada.
        if (userRepository.existsByEmail("demo@app.com")) {
            return;
        }

        User demoUser = User.builder()
                .email("demo@app.com")
                .password(passwordEncoder.encode("demo123"))
                .name("Demo User")
                .build();
        demoUser = userRepository.save(demoUser);

        // TODO: [PREENCHER AQUI] Adicionar mais dados de exemplo para User

        UserPreferences preferences = UserPreferences.builder()
                .userId(demoUser.getId())
                .styleWeights("{\"streetwear\": 0.8, \"casual\": 0.6, \"minimalist\": 0.5, \"sporty\": 0.4}")
                .gender("Male")
                .ageRange("25-34")
                .budgetRange("Medium-High")
                .build();
        userPreferencesRepository.save(preferences);

        // TODO: [PREENCHER AQUI] Adicionar mais dados de exemplo para UserPreferences

        // --- Início WardrobeItems ---

        // --- Tops ---
        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/tshirt-white-basic.webp")
                .category("tops")
                .subcategory("T-Shirt")
                .brand("Uniqlo")
                .color("White")
                .fit("Regular")
                .material("Cotton")
                .season("[\"spring\", \"summer\"]")
                .styleTags("[\"casual\", \"minimalist\"]")
                .timesUsed(42)
                .favoriteScore(4.8)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/hoodie-black-oversized.webp")
                .category("tops")
                .subcategory("Hoodie")
                .brand("Nike")
                .color("Black")
                .fit("Oversized")
                .material("Cotton/Polyester")
                .season("[\"fall\", \"winter\"]")
                .styleTags("[\"streetwear\", \"sporty\"]")
                .timesUsed(25)
                .favoriteScore(4.5)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/shirt-flannel-checkered.webp")
                .category("tops")
                .subcategory("Shirt")
                .brand("Carhartt")
                .color("Red/Navy")
                .fit("Relaxed")
                .material("Flannel")
                .season("[\"fall\", \"winter\", \"spring\"]")
                .styleTags("[\"casual\", \"vintage\"]")
                .timesUsed(12)
                .favoriteScore(3.9)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/sweater-grey-woo.webp")
                .category("tops")
                .subcategory("Sweater")
                .brand("Arket")
                .color("Grey")
                .fit("Regular")
                .material("Merino Wool")
                .season("[\"winter\", \"fall\"]")
                .styleTags("[\"minimalist\", \"formal\"]")
                .timesUsed(8)
                .favoriteScore(4.2)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/tshirt-graphic-vintage.webp")
                .category("tops")
                .subcategory("T-Shirt")
                .brand("Thrifted")
                .color("Washed Black")
                .fit("Boxy")
                .material("Cotton")
                .season("[\"summer\", \"spring\"]")
                .styleTags("[\"streetwear\", \"vintage\"]")
                .timesUsed(30)
                .favoriteScore(4.7)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/polo-navy-classic.webp")
                .category("tops")
                .subcategory("Polo")
                .brand("Ralph Lauren")
                .color("Navy")
                .fit("Slim")
                .material("Pique Cotton")
                .season("[\"summer\", \"spring\"]")
                .styleTags("[\"casual\", \"luxury\"]")
                .timesUsed(5)
                .favoriteScore(3.5)
                .build());

        // --- Bottoms ---
        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/jeans-blue-straight.webp")
                .category("bottoms")
                .subcategory("Jeans")
                .brand("Levi's")
                .color("Light Blue")
                .fit("Straight")
                .material("Denim")
                .season("[\"spring\", \"summer\", \"fall\", \"winter\"]")
                .styleTags("[\"casual\", \"vintage\"]")
                .timesUsed(50)
                .favoriteScore(5.0)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/cargo-pants-olive.webp")
                .category("bottoms")
                .subcategory("Cargo Pants")
                .brand("Dickies")
                .color("Olive Green")
                .fit("Loose")
                .material("Cotton Twill")
                .season("[\"fall\", \"spring\"]")
                .styleTags("[\"streetwear\", \"casual\"]")
                .timesUsed(18)
                .favoriteScore(4.3)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/chinos-beige-slim.webp")
                .category("bottoms")
                .subcategory("Chinos")
                .brand("Zara")
                .color("Beige")
                .fit("Slim")
                .material("Cotton Blend")
                .season("[\"spring\", \"summer\", \"fall\"]")
                .styleTags("[\"casual\", \"formal\"]")
                .timesUsed(10)
                .favoriteScore(3.8)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/shorts-black-mesh.webp")
                .category("bottoms")
                .subcategory("Shorts")
                .brand("Adidas")
                .color("Black")
                .fit("Relaxed")
                .material("Synthetic")
                .season("[\"summer\"]")
                .styleTags("[\"sporty\", \"streetwear\"]")
                .timesUsed(22)
                .favoriteScore(4.1)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/trousers-black-wool.webp")
                .category("bottoms")
                .subcategory("Trousers")
                .brand("COS")
                .color("Black")
                .fit("Straight")
                .material("Wool")
                .season("[\"winter\", \"fall\", \"spring\"]")
                .styleTags("[\"minimalist\", \"formal\", \"luxury\"]")
                .timesUsed(6)
                .favoriteScore(4.6)
                .build());

        // --- Jackets (Outerwear) ---
        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/jacket-denim-blue.webp")
                .category("jackets")
                .subcategory("Denim Jacket")
                .brand("Wrangler")
                .color("Blue")
                .fit("Regular")
                .material("Denim")
                .season("[\"spring\", \"fall\"]")
                .styleTags("[\"casual\", \"vintage\"]")
                .timesUsed(15)
                .favoriteScore(4.0)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/jacket-bomber-black.webp")
                .category("jackets")
                .subcategory("Bomber Jacket")
                .brand("Alpha Industries")
                .color("Black")
                .fit("Regular")
                .material("Nylon")
                .season("[\"fall\", \"winter\"]")
                .styleTags("[\"streetwear\", \"sporty\"]")
                .timesUsed(12)
                .favoriteScore(4.4)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/coat-camel-wool.webp")
                .category("jackets")
                .subcategory("Overcoat")
                .brand("Massimo Dutti")
                .color("Camel")
                .fit("Tailored")
                .material("Wool Blend")
                .season("[\"winter\"]")
                .styleTags("[\"formal\", \"luxury\"]")
                .timesUsed(4)
                .favoriteScore(4.9)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/puffer-grey-northface.webp")
                .category("jackets")
                .subcategory("Puffer Jacket")
                .brand("The North Face")
                .color("Grey")
                .fit("Puffy")
                .material("Down/Synthetic")
                .season("[\"winter\"]")
                .styleTags("[\"streetwear\", \"sporty\"]")
                .timesUsed(20)
                .favoriteScore(4.7)
                .build());

        // --- Shoes ---
        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/sneakers-white-jordan1.webp")
                .category("shoes")
                .subcategory("Sneakers")
                .brand("Jordan")
                .color("White/Grey")
                .fit("N/A")
                .material("Leather")
                .season("[\"spring\", \"summer\", \"fall\", \"winter\"]")
                .styleTags("[\"streetwear\", \"sporty\"]")
                .timesUsed(35)
                .favoriteScore(5.0)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/boots-black-leather.webp")
                .category("shoes")
                .subcategory("Boots")
                .brand("Dr. Martens")
                .color("Black")
                .fit("N/A")
                .material("Leather")
                .season("[\"winter\", \"fall\"]")
                .styleTags("[\"casual\", \"vintage\"]")
                .timesUsed(14)
                .favoriteScore(4.2)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/sneakers-grey-newbalance.webp")
                .category("shoes")
                .subcategory("Sneakers")
                .brand("New Balance")
                .color("Grey")
                .fit("N/A")
                .material("Suede/Mesh")
                .season("[\"spring\", \"summer\", \"fall\"]")
                .styleTags("[\"casual\", \"streetwear\"]")
                .timesUsed(28)
                .favoriteScore(4.8)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/shoes-loafers-black.webp")
                .category("shoes")
                .subcategory("Loafers")
                .brand("G.H. Bass")
                .color("Black")
                .fit("N/A")
                .material("Leather")
                .season("[\"spring\", \"summer\", \"fall\"]")
                .styleTags("[\"formal\", \"minimalist\"]")
                .timesUsed(5)
                .favoriteScore(3.7)
                .build());

        // --- Accessories ---
        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/accessory-beanie-black.webp")
                .category("accessories")
                .subcategory("Beanie")
                .brand("Carhartt WIP")
                .color("Black")
                .fit("N/A")
                .material("Acrylic")
                .season("[\"winter\", \"fall\"]")
                .styleTags("[\"streetwear\", \"casual\"]")
                .timesUsed(18)
                .favoriteScore(4.0)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/accessory-watch-silver.webp")
                .category("accessories")
                .subcategory("Watch")
                .brand("Seiko")
                .color("Silver")
                .fit("N/A")
                .material("Stainless Steel")
                .season("[\"spring\", \"summer\", \"fall\", \"winter\"]")
                .styleTags("[\"luxury\", \"formal\", \"casual\"]")
                .timesUsed(30)
                .favoriteScore(4.9)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/accessory-cap-navy.webp")
                .category("accessories")
                .subcategory("Cap")
                .brand("New Era")
                .color("Navy")
                .fit("N/A")
                .material("Cotton")
                .season("[\"summer\", \"spring\"]")
                .styleTags("[\"sporty\", \"streetwear\"]")
                .timesUsed(25)
                .favoriteScore(4.3)
                .build());

        wardrobeItemRepository.save(WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("/uploads/accessory-sunglasses-black.webp")
                .category("accessories")
                .subcategory("Sunglasses")
                .brand("Ray-Ban")
                .color("Black")
                .fit("N/A")
                .material("Acetate")
                .season("[\"summer\", \"spring\"]")
                .styleTags("[\"casual\", \"luxury\"]")
                .timesUsed(15)
                .favoriteScore(4.5)
                .build());

        // TODO: [PREENCHER AQUI] Adicionar mais dados de exemplo para WardrobeItem

        // --- SwipeResults ---
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_tshirt_white_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_hoodie_black_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_jeans_blue_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_sneakers_jordan_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_suit_formal_001").liked(false).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_dress_floral_001").liked(false).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_cargo_olive_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_boots_leather_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_shirt_hawaiian_001").liked(false).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_puffer_northface_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_beanie_carhartt_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_loafers_classic_001").liked(false).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_jacket_denim_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_shorts_adidas_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_coat_camel_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_watch_seiko_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_sweatpants_grey_001").liked(true).build());
        swipeResultRepository.save(SwipeResult.builder().userId(demoUser.getId()).imageId("img_blazer_slim_001").liked(false).build());

        // TODO: [PREENCHER AQUI] Adicionar mais dados de exemplo para SwipeResult
    }
}
