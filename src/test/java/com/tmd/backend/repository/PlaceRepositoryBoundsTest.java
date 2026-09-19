package com.tmd.backend.repository;

import com.tmd.backend.config.QuerydslConfig;
import com.tmd.backend.domain.place.Place;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class PlaceRepositoryBoundsTest {
    @Autowired TestEntityManager em;
    @Autowired PlaceRepository placeRepository;

    @Test
    void bbox_안의_활성_장소만_조회한다() {
        Place inside = em.persist(place("inside", 127.5, 37.5));
        em.persist(place("outside-longitude", 128.5, 37.5));
        em.persist(place("outside-latitude", 127.5, 38.5));
        Place inactive = place("inactive", 127.6, 37.6);
        inactive.deactivate();
        em.persist(inactive);
        em.flush();
        em.clear();

        List<Place> result = placeRepository.findPlacesWithinBounds(
            37.0,
            127.0,
            38.0,
            128.0
        );

        assertThat(result).extracting(Place::getContentId).containsExactly(inside.getContentId());
    }

    @Test
    void 선택한_분류_단계의_코드로_bbox_장소를_조회한다() {
        em.persist(place("cafe", 127.5, 37.5, "FD", "FD05", "FD050100"));
        em.persist(place("restaurant", 127.6, 37.6, "FD", "FD01", "FD010100"));
        em.persist(place("hotel", 127.7, 37.7, "AC", "AC01", "AC010100"));
        em.flush();
        em.clear();

        assertThat(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, 1, "FD"
        )).extracting(Place::getContentId).containsExactlyInAnyOrder("cafe", "restaurant");
        assertThat(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, 2, "FD05"
        )).extracting(Place::getContentId).containsExactly("cafe");
        assertThat(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, 3, "FD050100"
        )).extracting(Place::getContentId).containsExactly("cafe");
    }

    private Place place(String contentId, double mapX, double mapY) {
        return place(contentId, mapX, mapY, null, null, null);
    }

    private Place place(String contentId, double mapX, double mapY,
                        String level1, String level2, String level3) {
        return Place.create(
            contentId,
            null,
            null,
            null,
            contentId,
            mapX,
            mapY,
            null,
            null,
            null,
            null,
            null,
            level1,
            level2,
            level3
        );
    }
}
