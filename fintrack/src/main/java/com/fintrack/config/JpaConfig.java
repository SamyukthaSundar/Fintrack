package com.fintrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * JPA Auditing Configuration
 * Owner: Saanvi Kakkar
 * Enables @CreatedDate / @LastModifiedDate on entities.
 */
@Configuration
@EnableJpaAuditing
@EnableAsync
public class JpaConfig {
}
