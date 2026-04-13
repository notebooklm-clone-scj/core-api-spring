package core_api.global.security;

import core_api.global.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;

// JWT 파싱/검증 단계에서 발생한 인증 실패를 Spring Security 예외 흐름으로 넘기기 위한 예외
public class JwtAuthenticationException extends AuthenticationException {

    private final ErrorCode errorCode;

    public JwtAuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
