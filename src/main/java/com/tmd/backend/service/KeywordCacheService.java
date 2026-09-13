package com.tmd.backend.service;

import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordCacheService {
    private static final String KEY_PREFIX_FOR_KEYWORD_SEARCHING = "keyword-fetched:";

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // setIfAbsent는 atomic — 동시 요청 중 최초 1개만 true 반환
    // 해당 키가 없으면 저장하고 true, 해당 키가 이미 존재하면 false 반환
    public boolean tryMarkFetched(String keyword) {
        Boolean set = redisTemplate.opsForValue()
            .setIfAbsent(KEY_PREFIX_FOR_KEYWORD_SEARCHING + keyword, "true", Duration.ofDays(14));
        return Boolean.TRUE.equals(set);
    }

    public List<PlaceMarkerResponse> getCacheByKeywordPlaces(String keyword){
        Object raw = redisTemplate.opsForValue().get(KEY_PREFIX_FOR_KEYWORD_SEARCHING + keyword);
        return objectMapper.convertValue(
            raw,
            new TypeReference<List<PlaceMarkerResponse>>() {}
        );
    }

    public void setCacheByKeywordPlaces(List<PlaceMarkerResponse> list, String keyword){
        redisTemplate.opsForValue().set(
            KEY_PREFIX_FOR_KEYWORD_SEARCHING + keyword,
            list,
            Duration.ofDays(14)
        );
    }
}
