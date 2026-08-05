package com.tmd.backend.auth.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public JwtTokenProvider(@Value("${jwt.secret-key}") String secretKey){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.secretKey=Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String email){
        return createToken(email, accessExpiration);
    }

    public String createRefreshToken(String email){
        return createToken(email, refreshExpiration);
    }

    private String createToken(String email, long expiration){
        Claims claims = Jwts.claims().subject(email).build();
        Date now = new Date();

        return Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiration))
            .signWith(secretKey)
            .compact();
    }

    //  validateToken 삭제.
    // getClaims가 정상 return 되면 파싱 성공, Exception 발생시 Filter에서 처리.
    public Claims getClaims(String token){
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String getEmail(String token) {
        return getClaims(token)
            .getSubject();
    }

    public long getRefreshExpiration(){
        return refreshExpiration;
    }
}
