package core_api.global.security;

import core_api.domain.user.Role;
import core_api.domain.user.User;
import core_api.domain.user.UserRepository;
import core_api.global.exception.ErrorCode;
import core_api.global.jwt.JwtProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 토큰이 없는 요청은 그대로 다음 단계
        // 실제 보호 여부는 SecurityConfig의 URL 권한 설정이 결정
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            securityAuthenticationEntryPoint.commence(
                    request,
                    response,
                    new JwtAuthenticationException(ErrorCode.INVALID_TOKEN)
            );
            return;
        }

        try {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            Long userId = jwtProvider.extractUserId(token);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new JwtAuthenticationException(ErrorCode.INVALID_TOKEN));

            // 인증 주체 식별자는 토큰에서 꺼낸 userId를 그대로 사용
            // DB 조회는 role 확인 및 유효 사용자 검증 용도
            AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, resolveRole(user));
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().name()))
                    );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException | JwtAuthenticationException e) {
            SecurityContextHolder.clearContext();
            ErrorCode errorCode = e instanceof JwtAuthenticationException jwtAuthenticationException
                    ? jwtAuthenticationException.getErrorCode()
                    : ErrorCode.INVALID_TOKEN;

            securityAuthenticationEntryPoint.commence(
                    request,
                    response,
                    new JwtAuthenticationException(errorCode)
            );
        }
    }

    private Role resolveRole(User user) {
        // 기존 데이터 중 role이 비어 있는 사용자가 있다면 일반 사용자로 간주
        return user.getRole() == null ? Role.USER : user.getRole();
    }
}
