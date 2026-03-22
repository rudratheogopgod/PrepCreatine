package com.prepcreatine.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway configuration.
 * validate-on-migrate: true — fail on startup if applied migrations have been modified.
 * baseline-on-migrate: false — never auto-baseline an existing schema.
 * [FORBIDDEN] Never use flyway.clean in production.
 */
@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfig {
    // Spring Boot auto-configures Flyway from application.properties.
    // This class exists as a placeholder for future Flyway callbacks if needed.
    // All configuration is declared in application.properties under spring.flyway.*
}
