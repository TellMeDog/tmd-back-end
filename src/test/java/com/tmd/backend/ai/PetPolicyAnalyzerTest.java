package com.tmd.backend.ai;

import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@Disabled("외부 NVIDIA API를 호출하는 수동 통합 테스트")
@SpringBootTest
class PetPolicyAnalyzerTest {

    @Autowired
    private PetPolicyAnalyzer petPolicyAnalyzer;

    @Test
    void 반려동물_출입정책_분석() {

        Place place = mock(Place.class);
        given(place.getId()).willReturn(1L);

        PlacePetInfo petInfo = mock(PlacePetInfo.class);
        given(petInfo.getPlace()).willReturn(place);
        given(petInfo.getRelaAcdntRiskMtr()).willReturn("맹견은 입장 불가능합니다.");
        given(petInfo.getAcmpyPsblCpam()).willReturn("1인 당 최대 한 마리만 동반 가능합니다.");
        given(petInfo.getAcmpyNeedMtr()).willReturn("리드줄 착용 및 예방접종이 필요합니다.");
        given(petInfo.getEtcAcmpyInfo()).willReturn("반려견 동반 시 사전 문의 필수입니다.");

/*        ```json (결과)
        {
            "전체 견종 허용 여부": false,
            "최대 허용 체중": null,
            "목줄 필요 여부": true,
            "입마개 필요 여부": null,
            "사전 문의 필요 여부": true,
            "최대 반려동물 수": 1
        }
```
*/

        List<PetPolicyAnalysis> result = petPolicyAnalyzer.analyze(List.of(petInfo));

        System.out.println(result);
    }
}
