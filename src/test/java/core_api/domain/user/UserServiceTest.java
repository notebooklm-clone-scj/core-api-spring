package core_api.domain.user;

import core_api.domain.user.dto.UserLoginRequest;
import core_api.domain.user.dto.UserLoginResponse;
import core_api.domain.user.dto.UserLogoutResponse;
import core_api.domain.user.dto.UserSignupRequest;
import core_api.domain.user.dto.UserTokenRefreshRequest;
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

    @Mock
    private RefreshTokenService refreshTokenService;

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
    @DisplayName("로그인 성공 시 access token과 refresh token을 정상적으로 반환")
    void login_success(){
        //given
        UserLoginRequest request = new UserLoginRequest("test@gmail.com", "password123!");
        User fakeUser = User.builder()
                .email("test@gmail.com")
                .password("password123!")
                .nickname("테스터")
                .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(fakeUser));
        given(jwtProvider.createAccessToken(any())).willReturn("fake-access-token");
        given(jwtProvider.createRefreshToken(any())).willReturn("fake-refresh-token");
        //when
        UserLoginResponse response = userService.login(request);

        //then
        assertThat(response.getToken()).isEqualTo("fake-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("fake-refresh-token");
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

    @Test
    @DisplayName("리프레시 토큰 재발급 시 access token과 refresh token이 새로 발급된다")
    void refresh_success() {
        // given
        UserTokenRefreshRequest request = new UserTokenRefreshRequest("old-refresh-token");

        given(jwtProvider.extractRefreshTokenUserId("old-refresh-token")).willReturn(1L);
        given(jwtProvider.createAccessToken(1L)).willReturn("new-access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("new-refresh-token");

        // when
        UserLoginResponse response = userService.refresh(request);

        // then
        verify(refreshTokenService).validateRefreshToken(1L, "old-refresh-token");
        verify(refreshTokenService).saveRefreshToken(1L, "new-refresh-token");
        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("로그아웃 시 Redis의 refresh token을 삭제한다")
    void logout_success() {
        // given
        Long userId = 1L;

        // when
        UserLogoutResponse response = userService.logout(userId);

        // then
        verify(refreshTokenService).deleteRefreshToken(userId);
        assertThat(response.getMessage()).isEqualTo("로그아웃이 완료되었습니다.");
    }


}
