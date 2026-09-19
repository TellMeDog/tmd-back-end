package com.tmd.backend.service.place.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tourapi.category-sync.run-once", havingValue = "true")
public class TourCategorySyncRunner implements ApplicationRunner {
    private final TourCategorySyncService syncService;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting one-shot TourAPI category synchronization");
        TourCategorySyncSummary summary = syncService.synchronize();
        if (summary == null) {
            throw new IllegalStateException("TourAPI category synchronization is running on another instance");
        }
        int exitCode = SpringApplication.exit(applicationContext);
        System.exit(exitCode);
    }
}
