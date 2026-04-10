package core_api.domain.user;

import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import core_api.global.jwt.JwtProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserService userService;

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
