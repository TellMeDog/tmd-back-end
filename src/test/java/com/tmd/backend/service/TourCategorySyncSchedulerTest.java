package com.tmd.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TourCategorySyncSchedulerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(TourCategorySyncService.class, () -> mock(TourCategorySyncService.class))
        .withUserConfiguration(TourCategorySyncScheduler.class);

    @Test
    void defaultsToSundayAtFourThirtyInKorea() throws NoSuchMethodException {
        Method synchronize = TourCategorySyncScheduler.class.getDeclaredMethod("synchronize");
        Scheduled scheduled = synchronize.getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${tourapi.category-sync.cron:0 30 4 * * SUN}");
        assertThat(scheduled.zone()).isEqualTo("${tourapi.category-sync.zone:Asia/Seoul}");
    }

    @Test
    void createsSchedulerOnlyWhenEnabled() {
        contextRunner.withPropertyValues("tourapi.category-sync.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(TourCategorySyncScheduler.class));
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(TourCategorySyncScheduler.class));
    }
}
