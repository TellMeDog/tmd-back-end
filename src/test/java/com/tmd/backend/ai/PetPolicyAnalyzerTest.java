package com.tmd.backend.ai;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PetPolicyAnalyzerTest {

    @Autowired
    private PetPolicyAnalyzer petPolicyAnalyzer;

    @Test
    void 반려동물_출입정책_분석() {

        PetPolicyInput input = new PetPolicyInput(
            "전 견종 동반 가능",
            "일부구역 동반가능",
            "전 견종 동반 가능",
            "목줄 착용",
            """
            - 맨발 걷기 황토길은 반려견 출입 불가
            - 인식표 착용 요망
            - 맹견의 경우, 입마개 착용 필수
            - 배변봉투 지참 및 배변처리 필수
            """
        );

        PetPolicyAnalysis result = petPolicyAnalyzer.analyze(input);

        System.out.println("========== Gemini 분석 결과 ==========");
        System.out.println("accessScope = " + result.accessScope());
        System.out.println("conditions = " + result.conditions());
        System.out.println("maxPetsPerPerson = " + result.maxPetsPerPerson());
        System.out.println("weightLimit = " + result.weightLimit());
        System.out.println("exceptions = " + result.exceptions());
        System.out.println("summary = " + result.summary());
    }
}
