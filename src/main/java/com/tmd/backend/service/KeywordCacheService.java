package com.tmd.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class KeywordCacheService {
    private static final String KEY_PREFIX_FOR_KEYWORD_SEARCHING = "keyword-fetched";

    private final RedisTemplate<String, Object> redisTemplate;

    // setIfAbsent는 atomic — 동시 요청 중 최초 1개만 true 반환
    public boolean tryMarkFetched(String keyword) {
        Boolean set = redisTemplate.opsForValue()
            .setIfAbsent(KEY_PREFIX_FOR_KEYWORD_SEARCHING + keyword, "true", Duration.ofDays(14));
        return Boolean.TRUE.equals(set);
    }
}
