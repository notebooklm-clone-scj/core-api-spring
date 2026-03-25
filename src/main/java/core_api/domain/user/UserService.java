package core_api.domain.user;

import core_api.global.jwt.JwtProvider;
import lombok.Getter;
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
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
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
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        return jwtProvider.createToken(user.getId());
    }
}
