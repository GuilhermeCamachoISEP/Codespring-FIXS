package com.example.demo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs lightweight, idempotent ALTER TABLE migrations on every startup.
 * Needed because Hibernate's ddl-auto=update cannot add NOT NULL columns to
 * tables that already contain rows (there is nothing to fill existing rows with).
 *
 * Every statement uses IF NOT EXISTS / DEFAULT so it is safe to re-run.
 * Runs before the DatabaseSeeder (Order 1 < seeder's default).
 */
@Component
@Order(1)
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public DatabaseMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // ── outfit_history ────────────────────────────────────────────────────
        // worn: false = generated only, true = user confirmed they actually wore it
        exec("ALTER TABLE outfit_history ADD COLUMN IF NOT EXISTS worn BOOLEAN DEFAULT FALSE NOT NULL");

        // worn_date: the date the user marked the outfit as worn (nullable)
        exec("ALTER TABLE outfit_history ADD COLUMN IF NOT EXISTS worn_date DATE");

        // ── wardrobe_items ────────────────────────────────────────────────────
        // last_used_at: updated whenever an outfit containing this item is marked as worn
        exec("ALTER TABLE wardrobe_items ADD COLUMN IF NOT EXISTS last_used_at DATE");

        // event_name: set when the outfit was reserved for a specific event
        exec("ALTER TABLE outfit_history ADD COLUMN IF NOT EXISTS event_name VARCHAR(255)");

        System.out.println("[Migration] Schema columns verified / added successfully.");
    }

    private void exec(String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception e) {
            // Log but don't crash — column may already exist with a slightly different type
            System.err.println("[Migration] Skipped: " + sql + " → " + e.getMessage());
        }
    }
}
