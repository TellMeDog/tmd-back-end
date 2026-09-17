package com.tmd.backend.dto.response.place;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceDetailResponseTest {

    private final JdkSerializationRedisSerializer serializer =
        new JdkSerializationRedisSerializer();

    @Test
    void visitStats는_Redis_JDK_직렬화를_지원한다() {
        PlaceDetailResponse.VisitStats visitStats = PlaceDetailResponse.VisitStats.builder()
            .enteredCount(22)
            .mismatchedCount(12)
            .deniedCount(1)
            .lastReportedAt("2026-08-22T10:15:30")
            .topBreeds(List.of("말티즈", "푸들"))
            .build();

        byte[] serialized = serializer.serialize(visitStats);
        Object restored = serializer.deserialize(serialized);

        assertThat(restored)
            .isInstanceOfSatisfying(PlaceDetailResponse.VisitStats.class, stats -> {
                assertThat(stats.getEnteredCount()).isEqualTo(22);
                assertThat(stats.getMismatchedCount()).isEqualTo(12);
                assertThat(stats.getDeniedCount()).isEqualTo(1);
                assertThat(stats.getLastReportedAt()).isEqualTo("2026-08-22T10:15:30");
                assertThat(stats.getTopBreeds()).containsExactly("말티즈", "푸들");
            });
    }
}
