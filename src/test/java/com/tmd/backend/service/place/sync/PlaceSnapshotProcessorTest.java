package com.tmd.backend.service.place.sync;

import com.tmd.backend.common.PetInfoStatus;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetInfo;
import com.tmd.backend.external.tourapi.TourApiPlaceItem;
import com.tmd.backend.repository.place.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlaceSnapshotProcessorTest {
    private final PlaceRepository repository = mock(PlaceRepository.class);
    private final PlaceSnapshotProcessor processor = new PlaceSnapshotProcessor(repository);

    @Test
    void 변경된_장소를_갱신하고_응답에서_사라진_장소를_비활성화한다() {
        Place changed = Place.create("1", null, null, null, "이전 이름", 127.0, 37.0,
            null, null, "20260101000000", null, null, null, null, null);
        Place missing = Place.create("2", null, null, null, "사라진 장소", 127.0, 37.0,
            null, null, "20260101000000", null, null, null, null, null);
        when(repository.findAllForSync()).thenReturn(List.of(changed, missing));

        TourApiPlaceItem item = item("1", "변경된 이름", "20260916030000", "1");
        PlaceSnapshotProcessor.SnapshotResult result = processor.apply(List.of(item));

        assertThat(result.changed()).isEqualTo(1);
        assertThat(result.deactivated()).isEqualTo(1);
        assertThat(changed.getTitle()).isEqualTo("변경된 이름");
        assertThat(changed.isActive()).isTrue();
        assertThat(missing.isActive()).isFalse();
        verify(repository).flush();
    }

    @Test
    void 중복_contentId가_오면_동기화를_거부한다() {
        TourApiPlaceItem item = item("1", "장소", "20260916030000", "1");
        when(repository.findAllForSync()).thenReturn(List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> processor.apply(List.of(item, item)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("중복");
    }

    @Test
    void contentId가_없으면_동기화를_거부한다() {
        TourApiPlaceItem item = item(null, "장소", "20260916030000", "1");
        when(repository.findAllForSync()).thenReturn(List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> processor.apply(List.of(item)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("누락");
    }

    @Test
    void 기존_정책이_있고_원본_버전이_같으면_초기_동기화_버전을_설정한다() {
        Place existing = Place.create("1", null, null, null, "장소", 127.0, 37.0,
            null, null, "20260916030000", null, null, null, null, null);
        PlacePetInfo info = PlacePetInfo.empty(existing, PetInfoStatus.SUCCESS);
        ReflectionTestUtils.setField(existing, "placePetInfo", info);
        when(repository.findAllForSync()).thenReturn(List.of(existing));

        processor.apply(List.of(item("1", "장소", "20260916030000", "1")));

        assertThat(existing.getPetInfoSyncedModifiedTime()).isEqualTo("20260916030000");
    }

    @Test
    void 비활성_장소가_다시_나타나면_상세_정책_재수집을_요청한다() {
        Place existing = Place.create("1", null, null, null, "장소", 127.0, 37.0,
            null, null, "20260916030000", null, null, null, null, null);
        existing.markPetInfoSynced();
        existing.deactivate();
        when(repository.findAllForSync()).thenReturn(List.of(existing));

        PlaceSnapshotProcessor.SnapshotResult result = processor.apply(
            List.of(item("1", "장소", "20260916030000", "1"))
        );

        assertThat(result.reactivated()).isEqualTo(1);
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.getPetInfoSyncedModifiedTime()).isNull();
    }

    @Test
    void 수정시각이_없는_기존_정책도_안정적인_동기화_버전을_갖는다() {
        Place existing = Place.create("1", null, null, null, "장소", 127.0, 37.0,
            null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(
            existing,
            "placePetInfo",
            PlacePetInfo.empty(existing, PetInfoStatus.SUCCESS)
        );
        when(repository.findAllForSync()).thenReturn(List.of(existing));

        processor.apply(List.of(item("1", "장소", null, "1")));

        assertThat(existing.getPetInfoSyncedModifiedTime()).isEqualTo("UNKNOWN");
        assertThat(existing.needsPetInfoSync(null)).isFalse();
    }

    private TourApiPlaceItem item(String contentId, String title, String modifiedTime, String showFlag) {
        TourApiPlaceItem item = new TourApiPlaceItem();
        ReflectionTestUtils.setField(item, "contentid", contentId);
        ReflectionTestUtils.setField(item, "title", title);
        ReflectionTestUtils.setField(item, "modifiedtime", modifiedTime);
        ReflectionTestUtils.setField(item, "showflag", showFlag);
        return item;
    }
}
