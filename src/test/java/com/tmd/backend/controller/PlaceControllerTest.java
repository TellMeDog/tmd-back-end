package com.tmd.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmd.backend.dto.response.place.PlaceMapMarkerResponse;
import com.tmd.backend.service.place.PlaceService;
import com.tmd.backend.dto.response.place.PlaceMapSearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceControllerTest {
    private final PlaceService placeService = mock(PlaceService.class);
    private final PlaceController controller = new PlaceController(placeService);

    @Test
    void 익명_principal은_이메일로_전달하지_않는다() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        when(placeService.searchByKeyword("공원", null, null))
            .thenReturn(PlaceMapSearchResponse.from(List.of()));

        controller.searchPlaces("공원", null, anonymous);

        verify(placeService).searchByKeyword("공원", null, null);
    }

    @Test
    void 홈지도도_익명_principal을_null_이메일로_전달한다() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        when(placeService.init(null, null, 127.0, 37.0)).thenReturn(List.of());

        controller.init(null, 127.0, 37.0, anonymous);

        verify(placeService).init(null, null, 127.0, 37.0);
    }

    @Test
    void 인증된_principal은_이메일로_전달한다() {
        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                "test@email.com",
                null,
                AuthorityUtils.NO_AUTHORITIES
            );
        when(placeService.searchByKeyword("공원", null, "test@email.com"))
            .thenReturn(PlaceMapSearchResponse.from(List.of()));

        controller.searchPlaces("공원", null, authentication);

        verify(placeService).searchByKeyword("공원", null, "test@email.com");
    }

    @Test
    void 지도검색_응답은_data에_totalCount와_markers를_포함한다() throws Exception {
        PlaceMapSearchResponse searchResponse = PlaceMapSearchResponse.from(List.of(
            new PlaceMapMarkerResponse(1L, 127.0, 37.0, "GREY", "TOUR")
        ));
        when(placeService.searchByKeyword("공원", null, null)).thenReturn(searchResponse);

        var response = controller.searchPlaces("공원", null, null);
        JsonNode json = new ObjectMapper().readTree(
            new ObjectMapper().writeValueAsString(response.getBody())
        );

        assertThat(json.at("/data/totalCount").asLong()).isEqualTo(1);
        assertThat(json.at("/data/markers").size()).isEqualTo(1);
        assertThat(json.at("/data/markers/0/placeId").asLong()).isEqualTo(1);
    }

    @Test
    void 한글_카테고리를_서비스에_전달한다() {
        when(placeService.searchByCategory(
            "축제", null, null,
            37.0, 127.0, 38.0, 128.0
        )).thenReturn(PlaceMapSearchResponse.from(List.of()));

        controller.getPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0,
            "축제", null, null
        );

        verify(placeService).searchByCategory(
            "축제", null, null,
            37.0, 127.0, 38.0, 128.0
        );
    }
}
