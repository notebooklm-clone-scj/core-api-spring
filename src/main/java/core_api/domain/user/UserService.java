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

        // 2. 전달받은 데이터로 User 엔티티 조립 (추후 비밀번호 암호화 예정)
        User newUser = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .nickname(request.getNickname())
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

        return jwtProvider.createToken(user.getId());
    }
}
