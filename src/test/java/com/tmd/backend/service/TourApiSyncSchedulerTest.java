package com.tmd.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TourApiSyncSchedulerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(TourApiSyncService.class, () -> mock(TourApiSyncService.class))
        .withUserConfiguration(TourApiSyncScheduler.class);

    @Test
    void 기본_스케줄은_한국시간_매일_오전_5시다() throws NoSuchMethodException {
        Method synchronize = TourApiSyncScheduler.class.getDeclaredMethod("synchronize");
        Scheduled scheduled = synchronize.getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${tourapi.sync.cron:0 0 5 * * *}");
        assertThat(scheduled.zone()).isEqualTo("${tourapi.sync.zone:Asia/Seoul}");
    }

    @Test
    void dev_설정의_기본값도_오전_5시다() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(
            "application-dev",
            new ClassPathResource("application-dev.yaml")
        );

        assertThat(sources)
            .extracting(source -> source.getProperty("tourapi.sync.cron"))
            .containsExactly("${TOUR_API_SYNC_CRON:0 0 5 * * *}");
        assertThat(sources)
            .extracting(source -> source.getProperty("tourapi.sync.zone"))
            .containsExactly("${TOUR_API_SYNC_ZONE:Asia/Seoul}");
    }

    @Test
    void 활성화_설정이_true이면_스케줄러를_생성한다() {
        contextRunner
            .withPropertyValues("tourapi.sync.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(TourApiSyncScheduler.class));
    }

    @Test
    void 활성화_설정이_없으면_스케줄러를_생성하지_않는다() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(TourApiSyncScheduler.class));
    }
}
