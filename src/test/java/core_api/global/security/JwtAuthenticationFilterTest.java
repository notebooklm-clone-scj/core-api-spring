package core_api.global.security;

import core_api.domain.user.User;
import core_api.domain.user.UserRepository;
import core_api.global.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 SecurityContext에 인증 정보가 저장된다")
    void doFilterInternal_success() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {};
        User adminUser = User.builder()
                .email("admin@test.com")
                .password("password123!")
                .nickname("관리자")
                .build();
        request.addHeader("Authorization", "Bearer admin-token");

        given(jwtProvider.extractUserId("admin-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        assertThat(authenticatedUser.userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("잘못된 JWT면 인증 엔트리포인트가 호출된다")
    void doFilterInternal_invalidToken_callsEntryPoint() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {};
        request.addHeader("Authorization", "Bearer broken-token");

        given(jwtProvider.extractUserId("broken-token")).willThrow(new io.jsonwebtoken.MalformedJwtException("bad token"));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        ArgumentCaptor<JwtAuthenticationException> exceptionCaptor =
                ArgumentCaptor.forClass(JwtAuthenticationException.class);
        verify(securityAuthenticationEntryPoint).commence(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.eq(response),
                exceptionCaptor.capture()
        );
        assertThat(exceptionCaptor.getValue().getErrorCode().getCode()).isEqualTo("U004");
    }
}
