package core_api.domain.user;

import core_api.domain.user.dto.UserLoginRequest;
import core_api.domain.user.dto.UserSignupRequest;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import core_api.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider  jwtProvider;

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
    public String login(UserLoginRequest request) {
        User user = userRepository.findByEmail((request.getEmail()))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 로그인 토큰은 기존과 동일하게 userId만 담습니다.
        // 관리자 권한 검사는 이후 admin API에서 DB 조회로 판별하는 방식으로 확장할 예정입니다.
        return jwtProvider.createToken(user.getId());
    }
}
