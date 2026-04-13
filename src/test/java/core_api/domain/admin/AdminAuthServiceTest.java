package core_api.domain.admin;

import core_api.domain.user.Role;
import core_api.domain.user.User;
import core_api.domain.user.UserRepository;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import core_api.global.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    @DisplayName("관리자 토큰이면 관리자 인증에 성공")
    void authenticateAdmin_success() {
        // given
        String authorizationHeader = "Bearer admin-token";
        User adminUser = User.builder()
                .email("admin@test.com")
                .password("password123!")
                .nickname("관리자")
                .role(Role.ADMIN)
                .build();

        given(jwtProvider.extractUserId("admin-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));

        // when
        User authenticatedUser = adminAuthService.authenticateAdmin(authorizationHeader);

        // then
        assertThat(authenticatedUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("일반 사용자 토큰이면 관리자 인증에서 예외가 발생")
    void authenticateAdmin_fail_when_user_role() {
        // given
        String authorizationHeader = "Bearer user-token";
        User normalUser = User.builder()
                .email("user@test.com")
                .password("password123!")
                .nickname("일반유저")
                .role(Role.USER)
                .build();

        given(jwtProvider.extractUserId("user-token")).willReturn(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(normalUser));

        // when
        // then
        assertThatThrownBy(() -> adminAuthService.authenticateAdmin(authorizationHeader))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADMIN_ACCESS_DENIED);
    }
}
