package com.prepcreatine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * Async configuration.
 * On JDK 21+, uses virtual threads for all @Async tasks
 * (email, source ingestion, RAG embedding, etc) — no OS-thread blocking.
 *
 * spring.threads.virtual.enabled=true in application.properties also enables
 * virtual threads for Tomcat request handling.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        // SimpleAsyncTaskExecutor backed by virtual threads (JDK 21+)
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("prepcreatine-async-");
        executor.setVirtualThreads(true); // Spring 6.1+ API, JDK 21+
        return executor;
    }
}
