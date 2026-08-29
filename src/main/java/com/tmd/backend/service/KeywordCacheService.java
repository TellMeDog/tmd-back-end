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

    public boolean isFetched(String keyword){
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX_FOR_KEYWORD_SEARCHING + keyword));
    }

    public void markFetched(String keyword){
        redisTemplate.opsForValue().set(KEY_PREFIX_FOR_KEYWORD_SEARCHING, "true", Duration.ofDays(14));
    }
}
