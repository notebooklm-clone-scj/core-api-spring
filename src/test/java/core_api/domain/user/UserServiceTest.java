package core_api.domain.user;

import core_api.domain.user.dto.UserLoginRequest;
import core_api.domain.user.dto.UserSignupRequest;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import core_api.global.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 시 role은 항상 USER로 저장")
    void signup_success_role_is_user() {
        // given
        UserSignupRequest request = new UserSignupRequest("test@gmail.com", "password123!", "테스터");
        User savedUser = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .role(Role.USER)
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        userService.signup(request);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("로그인 성공 시 JWT 토큰을 정상적으로 반환")
    void login_success(){
        //given
        UserLoginRequest request = new UserLoginRequest("test@gmail.com", "password123!");
        User fakeUser = User.builder()
                .email("test@gmail.com")
                .password("password123!")
                .nickname("테스터")
                .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(fakeUser));
        given(jwtProvider.createToken(any())).willReturn("fake-jwt-token");
        //when
        String token = userService.login(request);

        //then
        assertThat(token).isEqualTo("fake-jwt-token");
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 틀리면 예외 발생")
    void login_fail_wrong_password(){
        //given
        UserLoginRequest request = new UserLoginRequest("test@gmail.com", "wrong-password!");
        User fakeUser = User.builder()
                .email("test@gmail.com")
                .password("password123!")
                .nickname("테스터")
                .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(fakeUser));
        //when
        //then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }


}
