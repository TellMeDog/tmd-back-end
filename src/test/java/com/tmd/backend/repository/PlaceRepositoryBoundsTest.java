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

    private Place place(String contentId, double mapX, double mapY) {
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
            null,
            null,
            null
        );
    }
}
