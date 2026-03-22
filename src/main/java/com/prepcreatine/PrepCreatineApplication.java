package com.prepcreatine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * PrepCreatine Backend Application Entry Point.
 * "Creatine for your exam prep" — brought to you by Rudra Agrawal.
 *
 * @EnableAsync — required for @Async on email, source ingestion, analytics
 * @EnableCaching — required for @Cacheable on YouTube, Reddit, roadmap, etc.
 * Virtual threads enabled globally via spring.threads.virtual.enabled=true
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
public class PrepCreatineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrepCreatineApplication.class, args);
    }
}
