# Relatório de Implementação: DatabaseSeeder

Este documento detalha o processo de análise e criação do `DatabaseSeeder` para o projeto Codespring-FIXS.

## 1. Fase de Análise (Discovery)

Para garantir que o código seria compatível com a estrutura existente, realizei uma exploração das entidades e repositórios.

### Entidades Identificadas:
- **User** (`com.example.demo.domain.User`):
    - Campos: `id`, `email`, `password`, `name`, `createdAt`.
    - Repositório: `UserRepository` com métodos `existsByEmail` e `findByEmail`.
- **UserPreferences** (`com.example.demo.domain.UserPreferences`):
    - Campos: `userId` (FK), `styleWeights` (String/JSON), `gender`, `ageRange`, `budgetRange`.
    - Repositório: `UserPreferencesRepository`.
- **WardrobeItem** (`com.example.demo.domain.WardrobeItem`):
    - Campos: `userId`, `imageUrl`, `category`, `subcategory`, `brand`, `color`, `fit`, `material`, `season`, `styleTags`, `timesUsed`, `favoriteScore`.
    - Repositório: `WardrobeItemRepository`.
- **SwipeResult** (`com.example.demo.domain.SwipeResult`):
    - Campos: `userId`, `imageId`, `liked`.
    - Repositório: `SwipeResultRepository`.

### Infraestrutura de Segurança:
- Analisei o `SecurityConfig.java` para confirmar a existência do Bean `PasswordEncoder` (BCryptPasswordEncoder), necessário para encriptar a password do utilizador demo.

---

## 2. Raciocínio de Implementação

1.  **Localização**: O ficheiro foi criado em `src/main/java/com/example/demo/config/DatabaseSeeder.java`, integrando-se na camada de configuração da aplicação.
2.  **Idempotência**: Implementei uma verificação inicial usando `userRepository.existsByEmail("demo@app.com")`. Se o utilizador já existir, o seeder ignora a execução para evitar duplicados ou erros de violação de chave única.
3.  **Segurança**: A password "demo123" é codificada dinamicamente usando o `PasswordEncoder` injetado, seguindo as melhores práticas de segurança.
4.  **Associações**: Todos os dados de exemplo (`UserPreferences`, `WardrobeItem`, `SwipeResult`) são vinculados ao ID do utilizador criado dinamicamente, garantindo a integridade referencial.
5.  **Perfis**: Utilizei `@Profile("!prod")` para garantir que estes dados de teste nunca sejam inseridos num ambiente de produção.

---

## 3. Validação Técnica

Após a escrita do código, executei a validação de compilação:
- **Comando**: `./mvnw clean compile`
- **Resultado**: `BUILD SUCCESS`
- **Conclusão**: O código utiliza corretamente as anotações do Lombok (`@Builder`), as interfaces do Spring Data JPA e as dependências do Spring Security.

---

## 4. Resultado Final (Código)

```java
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
                .styleWeights("{\"streetwear\": 0.8, \"casual\": 0.6}")
                .gender("Unisex")
                .ageRange("20-30")
                .budgetRange("Medium")
                .build();
        userPreferencesRepository.save(preferences);

        // TODO: [PREENCHER AQUI] Adicionar mais dados de exemplo para UserPreferences

        WardrobeItem item = WardrobeItem.builder()
                .userId(demoUser.getId())
                .imageUrl("https://example.com/image.jpg")
                .category("Tops")
                .subcategory("T-Shirt")
                .brand("Demo Brand")
                .color("Black")
                .fit("Regular")
                .material("Cotton")
                .season("[\"spring\", \"summer\"]")
                .styleTags("[\"casual\", \"streetwear\"]")
                .timesUsed(5)
                .favoriteScore(4.5)
                .build();
        wardrobeItemRepository.save(item);

        // TODO: [PREENCHER AQUI] Adicionar mais dados de exemplo para WardrobeItem

        SwipeResult swipe = SwipeResult.builder()
                .userId(demoUser.getId())
                .imageId("img_001")
                .liked(true)
                .build();
        swipeResultRepository.save(swipe);

        // TODO: [PREENCHER AQUI] Adicionar mais dados de exemplo para SwipeResult
    }
}
```
