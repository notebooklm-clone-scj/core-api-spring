package core_api.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    private final Key key;
    private final long expiration;

    public JwtProvider(@Value("${jwt.secret}") String secretKey,
                       @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String createToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(userId.toString()) // 토큰의 주인 (유저 PK)
                .setIssuedAt(now)              // 발급 시간
                .setExpiration(validity)       // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 비밀
                .compact();
    }

    // 지금 프로젝트의 JWT는 subject에 userId만 담고 있으므로
    // 관리자 API에서는 이 값을 다시 꺼내 DB의 role과 대조해서 권한을 판별합니다.
    public Long extractUserId(String token) {
        String subject = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        return Long.valueOf(subject);
    }

}
