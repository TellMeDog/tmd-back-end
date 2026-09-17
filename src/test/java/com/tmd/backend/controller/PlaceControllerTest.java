package com.tmd.backend.controller;

import com.tmd.backend.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;

import static org.mockito.Mockito.*;

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
        when(placeService.searchByKeyword("공원", null, null, 127.0, 37.0))
            .thenReturn(List.of());

        controller.searchPlaces("공원", null, 127.0, 37.0, anonymous);

        verify(placeService).searchByKeyword("공원", null, null, 127.0, 37.0);
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
        when(placeService.searchByKeyword("공원", null, "test@email.com", 127.0, 37.0))
            .thenReturn(List.of());

        controller.searchPlaces("공원", null, 127.0, 37.0, authentication);

        verify(placeService).searchByKeyword("공원", null, "test@email.com", 127.0, 37.0);
    }
}
