package core_api.domain.user;

import core_api.domain.user.dto.UserLoginRequest;
import core_api.domain.user.dto.UserSignupRequest;
import core_api.domain.user.dto.UserLoginResponse;
import core_api.domain.user.dto.UserLogoutResponse;
import core_api.domain.user.dto.UserTokenRefreshRequest;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import core_api.global.jwt.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider  jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public Long signup(UserSignupRequest request) {
        // 1. 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. 회원가입으로 생성되는 계정은 항상 USER 권한만 부여합니다.
        // ADMIN 계정은 추후 운영자가 DB에서 직접 넣는 방식으로 관리합니다.
        User newUser = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .role(Role.USER)
                .build();

        // 3. DB에 저장 후, 자동 생성된 ID값 반환
        return userRepository.save(newUser).getId();
    }

    @Transactional
    public UserLoginResponse login(UserLoginRequest request) {
        User user = userRepository.findByEmail((request.getEmail()))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

        // token 필드는 프론트 하위 호환을 위해 access token을 그대로 유지합니다.
        return new UserLoginResponse(accessToken, refreshToken, resolveRole(user));
    }

    @Transactional(readOnly = true)
    public UserLoginResponse refresh(UserTokenRefreshRequest request) {
        try {
            Long userId = jwtProvider.extractRefreshTokenUserId(request.getRefreshToken());
            refreshTokenService.validateRefreshToken(userId, request.getRefreshToken());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            String newAccessToken = jwtProvider.createAccessToken(userId);
            String newRefreshToken = jwtProvider.createRefreshToken(userId);
            refreshTokenService.saveRefreshToken(userId, newRefreshToken);

            return new UserLoginResponse(newAccessToken, newRefreshToken, resolveRole(user));
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @Transactional
    public UserLogoutResponse logout(Long userId) {
        // 로그아웃은 Redis에 저장된 refresh token을 삭제해서
        // 더 이상 access token 재발급이 되지 않도록 막는 방식으로 처리합니다.
        refreshTokenService.deleteRefreshToken(userId);
        return new UserLogoutResponse("로그아웃이 완료되었습니다.");
    }

    private Role resolveRole(User user) {
        if (user.getRole() == null) {
            throw new CustomException(ErrorCode.USER_ROLE_MISSING);
        }
        return user.getRole();
    }
}
