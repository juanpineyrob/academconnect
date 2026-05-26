package com.academconnect.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** F15 — habilita el procesamiento asincrónico de eventos de actividad. */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "actividadExecutor")
    public Executor actividadExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(4);
        exec.setQueueCapacity(200);
        exec.setThreadNamePrefix("actividad-");
        exec.initialize();
        return exec;
    }
}
