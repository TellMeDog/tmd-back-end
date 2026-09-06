package com.tmd.backend.ai;

import com.tmd.backend.domain.place.PlacePetInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetPolicyAnalyzer {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public List<PetPolicyAnalysis> analyze(List<PlacePetInfo> placePetInfos) {
        List<PetPolicyInput> inputs = placePetInfos.stream()
            .map(PetPolicyInput::from)
            .toList();

        String inputJson;
        try {
            inputJson = objectMapper.writeValueAsString(inputs);
        } catch (JacksonException e) {
            throw new RuntimeException("정책 JSON 직렬화 실패", e);
        }

        return chatClient.prompt()
            .system("""
                당신은 반려동물 출입 정책을 분석하는 데이터 정규화 전문가입니다.
                입력된 JSON 배열의 각 장소 정책을 분석하여 반드시 JSON 배열로만 응답하세요.

                규칙:
                1. 입력된 정책만을 근거로 분석하세요.
                2. 정보가 명확하지 않으면 추측하지 마세요.
                3. 정책에 명시되지 않은 값은 null로 반환하세요.
                4. "전 견종"과 "맹견"을 별도로 구분지어서 판단하세요.
                5. "미만"과 "이하"를 구분하세요.
                6. 서로 다른 필드의 동일한 내용을 중복 해석하지 마세요.
                7. placeId는 입력값 그대로 반환하세요.
                8. 응답은 JSON 배열 외 다른 텍스트를 포함하지 마세요.
                """)
            .user(inputJson)
            .call()
            .entity(new ParameterizedTypeReference<List<PetPolicyAnalysis>>() {});
    }
}
