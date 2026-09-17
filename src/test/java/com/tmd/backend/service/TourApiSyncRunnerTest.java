package com.tmd.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TourApiSyncRunnerTest {
    private final TourApiSyncService syncService = mock(TourApiSyncService.class);
    private final ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
    private final TourApiSyncRunner runner = new TourApiSyncRunner(syncService, applicationContext);

    @Test
    void 동기화_실패를_정상_종료로_바꾸지_않고_전파한다() {
        ApplicationArguments arguments = mock(ApplicationArguments.class);
        when(syncService.synchronize()).thenThrow(new IllegalStateException("sync failed"));

        assertThatThrownBy(() -> runner.run(arguments))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("sync failed");

        verifyNoInteractions(applicationContext);
    }

    @Test
    void 실행_잠금을_얻지_못하면_성공으로_종료하지_않는다() {
        ApplicationArguments arguments = mock(ApplicationArguments.class);
        when(syncService.synchronize()).thenReturn(null);

        assertThatThrownBy(() -> runner.run(arguments))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("실행 중");

        verifyNoInteractions(applicationContext);
    }
}
