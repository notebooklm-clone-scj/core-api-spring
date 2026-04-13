package core_api.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    private final Key key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtProvider(@Value("${jwt.secret}") String secretKey,
                       @Value("${jwt.access-expiration}") long accessExpiration,
                       @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, TokenType.ACCESS, accessExpiration);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, TokenType.REFRESH, refreshExpiration);
    }

    private String createToken(Long userId, TokenType tokenType, long expiration) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(userId.toString()) // 토큰의 주인 (유저 PK)
                .claim("type", tokenType.name())
                .setIssuedAt(now)              // 발급 시간
                .setExpiration(validity)       // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 비밀
                .compact();
    }

    public Long extractAccessTokenUserId(String token) {
        return extractUserId(token, TokenType.ACCESS);
    }

    public Long extractRefreshTokenUserId(String token) {
        return extractUserId(token, TokenType.REFRESH);
    }

    private Long extractUserId(String token, TokenType expectedType) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, expectedType);

        return Long.valueOf(claims.getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private void validateTokenType(Claims claims, TokenType expectedType) {
        String type = claims.get("type", String.class);
        if (type == null || !expectedType.name().equals(type)) {
            throw new IllegalArgumentException("Invalid token type");
        }
    }

}
