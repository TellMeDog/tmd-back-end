package com.tmd.backend.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiClientTest {
    private MockRestServiceServer server;
    private TourApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TourApiClient(RestClient.builder(), Duration.ofSeconds(1), Duration.ofSeconds(1));
        ReflectionTestUtils.setField(client, "restClient", builder.build());
        ReflectionTestUtils.setField(client, "serviceKey", "test-key");
        ReflectionTestUtils.setField(client, "syncPageSize", 1);
        ReflectionTestUtils.setField(client, "syncArrange", "");
    }

    @Test
    void totalCount까지_모든_페이지를_조회한다() {
        server.expect(request -> assertThat(request.getURI().getQuery()).contains("pageNo=1", "numOfRows=1"))
            .andRespond(withSuccess(response("1", 1, 2), MediaType.APPLICATION_JSON));
        server.expect(request -> assertThat(request.getURI().getQuery()).contains("pageNo=2", "numOfRows=1"))
            .andRespond(withSuccess(response("2", 2, 2), MediaType.APPLICATION_JSON));

        List<TourApiPlaceItem> result = client.getTourSyncList();

        assertThat(result).extracting(TourApiPlaceItem::getContentid).containsExactly("1", "2");
        server.verify();
    }

    @Test
    void totalCount에_도달하기_전에_빈_페이지가_오면_실패한다() {
        server.expect(request -> assertThat(request.getURI().getQuery()).contains("pageNo=1"))
            .andRespond(withSuccess(response("1", 1, 2), MediaType.APPLICATION_JSON));
        server.expect(request -> assertThat(request.getURI().getQuery()).contains("pageNo=2"))
            .andRespond(withSuccess(emptyResponse(2, 2), MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::getTourSyncList)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("totalCount보다 먼저 종료");
        server.verify();
    }

    @Test
    void 대중소분류를_계층_파라미터로_조회한다() {
        server.expect(request -> assertThat(request.getURI().getQuery())
                .doesNotContain("lclsSystm1", "lclsSystm2"))
            .andRespond(withSuccess(categoryResponse("AC", "숙박", 1), MediaType.APPLICATION_JSON));
        server.expect(request -> assertThat(request.getURI().getQuery())
                .contains("lclsSystm1=AC")
                .doesNotContain("lclsSystm2"))
            .andRespond(withSuccess(categoryResponse("AC01", "호텔", 1), MediaType.APPLICATION_JSON));
        server.expect(request -> assertThat(request.getURI().getQuery())
                .contains("lclsSystm1=AC", "lclsSystm2=AC01"))
            .andRespond(withSuccess(categoryResponse("AC010100", "관광호텔", 1), MediaType.APPLICATION_JSON));

        List<TourApiCategoryEntry> result = client.getTourCategoryCodes();

        assertThat(result).containsExactly(
            new TourApiCategoryEntry("AC", "숙박", 1, null, 1),
            new TourApiCategoryEntry("AC01", "호텔", 2, "AC", 1),
            new TourApiCategoryEntry("AC010100", "관광호텔", 3, "AC01", 1)
        );
        server.verify();
    }

    @Test
    void 카테고리_API_오류코드를_실패로_처리한다() {
        server.expect(request -> assertThat(request.getURI().getPath()).endsWith("/lclsSystmCode2"))
            .andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"03","resultMsg":"NO_DATA"}}}
                """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::getTourCategoryCodes)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("03", "NO_DATA");
        server.verify();
    }

    private String response(String contentId, int pageNo, int totalCount) {
        return """
            {
              "response": {
                "header": {"resultCode": "0000", "resultMsg": "OK"},
                "body": {
                  "items": {"item": [{"contentid": "%s"}]},
                  "numOfRows": 1,
                  "pageNo": %d,
                  "totalCount": %d
                }
              }
            }
            """.formatted(contentId, pageNo, totalCount);
    }

    private String emptyResponse(int pageNo, int totalCount) {
        return """
            {
              "response": {
                "body": {
                  "items": {"item": []},
                  "numOfRows": 1,
                  "pageNo": %d,
                  "totalCount": %d
                }
              }
            }
            """.formatted(pageNo, totalCount);
    }

    private String categoryResponse(String code, String name, int rnum) {
        return """
            {
              "response": {
                "header": {"resultCode": "0000", "resultMsg": "OK"},
                "body": {
                  "items": {"item": [{"code": "%s", "name": "%s", "rnum": %d}]},
                  "numOfRows": 1000,
                  "pageNo": 1,
                  "totalCount": 1
                }
              }
            }
            """.formatted(code, name, rnum);
    }
}
