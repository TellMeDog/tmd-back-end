package com.tmd.backend.service.place;

import com.tmd.backend.ai.AccessScope;
import com.tmd.backend.ai.WeightLimitType;
import com.tmd.backend.common.PetInfoStatus;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlacePetPolicyClassifierTest {
    private final PlacePetPolicyClassifier classifier = new PlacePetPolicyClassifier();

    @Test
    void 데이터가_없으면_UNKNOWN으로_분류한다() {
        PlacePetInfo info = info(PetInfoStatus.NO_DATA, null, null, null, null);

        var result = classifier.classify(info);

        assertThat(result.analysis().accessScope()).isEqualTo(AccessScope.UNKNOWN);
        assertThat(result.analysis().weightLimitType()).isEqualTo(WeightLimitType.UNKNOWN);
        assertThat(result.requiresReview()).isFalse();
    }

    @Test
    void 전구역_체중과_목줄_조건을_분류한다() {
        PlacePetInfo info = info(
            PetInfoStatus.SUCCESS,
            "전 구역 동반 가능",
            "반려견 10kg 이하 동반 가능",
            "목줄 착용",
            null
        );

        var result = classifier.classify(info);

        assertThat(result.analysis().accessScope()).isEqualTo(AccessScope.ALL);
        assertThat(result.analysis().maxWeightKg()).isEqualTo(10.0);
        assertThat(result.analysis().weightLimitType()).isEqualTo(WeightLimitType.LESS_THAN_OR_EQUAL);
        assertThat(result.analysis().leashRequired()).isTrue();
    }

    @Test
    void 접종처럼_반려견정보로_판단할수_없는_조건은_YELLOW다() {
        PlacePetInfo info = info(
            PetInfoStatus.SUCCESS,
            "전 구역 동반 가능",
            "모든 견종 동반 가능",
            null,
            "예방 접종증 지참"
        );

        var result = classifier.classify(info);

        assertThat(result.analysis().defaultPolicy()).isEqualTo("YELLOW");
        assertThat(result.yellowReasons()).contains("vaccination");
    }

    private PlacePetInfo info(PetInfoStatus status, String type, String capacity, String need, String additional) {
        PlacePetInfo info = mock(PlacePetInfo.class);
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(1L);
        when(info.getPlace()).thenReturn(place);
        when(info.getStatus()).thenReturn(status);
        when(info.getAcmpyTypeCd()).thenReturn(type);
        when(info.getAcmpyPsblCpam()).thenReturn(capacity);
        when(info.getAcmpyNeedMtr()).thenReturn(need);
        when(info.getEtcAcmpyInfo()).thenReturn(additional);
        return info;
    }
}
