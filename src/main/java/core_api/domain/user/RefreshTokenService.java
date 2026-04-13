package core_api.domain.user;

import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // 사용자별 현재 유효한 refresh token 하나만 Redis에 저장합니다.
    // rotation 시에는 같은 key에 새 토큰을 덮어써서 이전 토큰을 즉시 무효화합니다.
    public void saveRefreshToken(Long userId, String refreshToken) {
        stringRedisTemplate.opsForValue().set(
                buildKey(userId),
                refreshToken,
                refreshExpiration,
                TimeUnit.MILLISECONDS
        );
    }

    public void validateRefreshToken(Long userId, String refreshToken) {
        String savedRefreshToken = stringRedisTemplate.opsForValue().get(buildKey(userId));

        if (savedRefreshToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    public void deleteRefreshToken(Long userId) {
        stringRedisTemplate.delete(buildKey(userId));
    }

    private String buildKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }
}
