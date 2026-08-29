package com.tmd.backend.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PetPolicyAnalyzer {

    private final ChatClient chatClient;

    public PetPolicyAnalysis analyze(PetPolicyInput input){
        return chatClient.prompt()
            .system("""
            당신은 반려동물 동반 여행 장소의 출입 정책을 분석하는 시스템입니다.

            입력된 정책만을 근거로 분석하세요.
            정보가 명확하지 않으면 추측하지 마세요.

            자연어 정책을 PetPolicyAnalysis 구조로 정형화하세요.

            다음 정보를 정확하게 추출하세요.
            - 전체 구역 또는 일부 구역의 동반 가능 여부
            - 목줄, 켄넬, 입마개, 이동장 등의 필수 조건
            - 예방접종 등의 필수 조건
            - 사전 문의 필요 여부
            - 맹견 등의 특정 견종 제한
            - 1인당 최대 반려동물 수
            - 체중 제한
            - 특정 시설이나 구역에 대한 예외적인 출입 제한

            최종적인 입장 가능 여부를 임의로 판단하지 마세요.
            입력된 정책에 존재하는 조건을 정확하게 추출하세요.
            """)
            .user("""
            사고/제한 사항: %s
            동반 가능 범위: %s
            동반 가능 조건: %s
            필요 사항: %s
            기타 안내: %s
            """.formatted(
                input.accidentRisk(),
                input.accompanyType(),
                input.possibleCapacity(),
                input.requiredItems(),
                input.additionalInfo()
            ))
            .call()
            .entity(PetPolicyAnalysis.class);
    }
}
