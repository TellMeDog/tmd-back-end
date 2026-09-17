package com.tmd.backend.service;

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
@ConditionalOnProperty(name = "tourapi.sync.run-once", havingValue = "true")
public class TourApiSyncRunner implements ApplicationRunner {
    private final TourApiSyncService syncService;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("TourAPI one-shot 동기화를 시작합니다.");
        PlaceSyncSummary summary = syncService.synchronize();
        if (summary == null) {
            throw new IllegalStateException("다른 인스턴스에서 TourAPI 동기화를 실행 중입니다.");
        }

        int exitCode = SpringApplication.exit(applicationContext);
        System.exit(exitCode);
    }
}
