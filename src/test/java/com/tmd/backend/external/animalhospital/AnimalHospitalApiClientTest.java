package com.tmd.backend.external.animalhospital;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AnimalHospitalApiClientTest {
    private MockRestServiceServer server;
    private AnimalHospitalApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AnimalHospitalApiClient(
            RestClient.builder(),
            "https://apis.data.go.kr/1741000/animal_hospitals/info",
            "test-key",
            Duration.ofSeconds(1),
            Duration.ofSeconds(1),
            Duration.ZERO
        );
        ReflectionTestUtils.setField(client, "restClient", builder.build());
    }

    @Test
    void requestsJsonPageAndMapsUppercaseFields() {
        server.expect(request -> assertThat(request.getURI().getQuery())
                .contains("pageNo=1", "numOfRows=100", "type=json", "serviceKey=test-key"))
            .andRespond(withSuccess("""
                {
                  "response": {
                    "header": {"resultCode": "0", "resultMsg": "정상"},
                    "body": {
                      "dataType": "JSON",
                      "items": {"item": [{
                        "MNG_NO": "321000001020260007",
                        "BPLC_NM": "방배하본동물의료센터",
                        "SALS_STTS_CD": "01",
                        "DTL_SALS_STTS_CD": "0000",
                        "CRD_INFO_X": "198787.489268795",
                        "CRD_INFO_Y": "442666.440681077"
                      }]},
                      "numOfRows": 100,
                      "pageNo": 1,
                      "totalCount": 1
                    }
                  }
                }
                """, MediaType.APPLICATION_JSON));

        List<AnimalHospitalItem> result = client.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().managementNumber()).isEqualTo("321000001020260007");
        assertThat(result.getFirst().isOpen()).isTrue();
        server.verify();
    }
}
