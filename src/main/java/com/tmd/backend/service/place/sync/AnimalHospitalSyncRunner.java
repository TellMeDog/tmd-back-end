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
@ConditionalOnProperty(name = "animal-hospital.sync.run-once", havingValue = "true")
public class AnimalHospitalSyncRunner implements ApplicationRunner {
    private final AnimalHospitalSyncService syncService;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting one-shot animal hospital synchronization");
        AnimalHospitalSyncSummary summary = syncService.synchronize();
        if (summary == null) {
            throw new IllegalStateException("Animal hospital synchronization is running in another instance");
        }
        int exitCode = SpringApplication.exit(applicationContext);
        System.exit(exitCode);
    }
}
