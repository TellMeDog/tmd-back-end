package com.tmd.backend.service.place.sync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AnimalHospitalSyncSchedulerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(AnimalHospitalSyncService.class, () -> mock(AnimalHospitalSyncService.class))
        .withUserConfiguration(AnimalHospitalSyncScheduler.class);

    @Test
    void defaultScheduleRunsDailyAtFiveThirtyInKorea() throws NoSuchMethodException {
        Method synchronize = AnimalHospitalSyncScheduler.class.getDeclaredMethod("synchronize");
        Scheduled scheduled = synchronize.getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${animal-hospital.sync.cron:0 30 5 * * *}");
        assertThat(scheduled.zone()).isEqualTo("${animal-hospital.sync.zone:Asia/Seoul}");
    }

    @Test
    void schedulerIsDisabledByDefaultAndEnabledByProperty() {
        contextRunner.run(context ->
            assertThat(context).doesNotHaveBean(AnimalHospitalSyncScheduler.class));
        contextRunner.withPropertyValues("animal-hospital.sync.enabled=true").run(context ->
            assertThat(context).hasSingleBean(AnimalHospitalSyncScheduler.class));
    }
}
