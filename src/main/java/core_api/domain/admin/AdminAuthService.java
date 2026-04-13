package core_api.domain.admin;

import core_api.domain.user.Role;
import core_api.domain.user.User;
import core_api.domain.user.UserRepository;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import core_api.global.jwt.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    // 관리자 API는 Spring Security를 아직 붙이지 않았기 때문에,
    // Authorization 헤더를 직접 읽어 userId를 추출하고 role을 확인합니다.
    public User authenticateAdmin(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new CustomException(ErrorCode.AUTH_HEADER_MISSING);
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        try {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            Long userId = jwtProvider.extractUserId(token);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            if (user.getRole() != Role.ADMIN) {
                throw new CustomException(ErrorCode.ADMIN_ACCESS_DENIED);
            }

            return user;
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}
