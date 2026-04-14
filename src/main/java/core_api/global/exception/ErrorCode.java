package core_api.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 유저 관련 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 이메일(아이디)입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U002", "비밀번호가 일치하지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U003", "이미 가입된 이메일입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "U004", "유효하지 않은 인증 토큰입니다."),
    AUTH_HEADER_MISSING(HttpStatus.UNAUTHORIZED, "U005", "인증 헤더가 필요합니다."),
    ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "U006", "관리자만 접근할 수 있습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "U007", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "U008", "저장된 리프레시 토큰이 없습니다."),
    USER_ROLE_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "U009", "사용자 권한 정보가 존재하지 않습니다."),

    // Notebook 관련 에러
    NOTEBOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "해당 노트북을 찾을 수 없습니다."),

    // PDF 관련 에러
    PDF_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P001", "PDF 텍스트 추출에 실패했습니다."),

    // 문서 관련 에러
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "해당 문서를 찾을 수 없습니다."),

    // 파일 관련 에러
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "파일을 읽는 중 에러가 발생했습니다."),

    // AI 관련 에러
    // -> 외부 AI Worker는 timeout, 빈 응답, 일반 통신 실패를 구분하는 편이 운영에 더 유리하다.
    AI_WORKER_ERROR(HttpStatus.BAD_GATEWAY, "A001", "AI 분석 서버와 통신 중 오류가 발생했습니다."),
    AI_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, "A002", "AI 분석 서버의 응답이 비어 있습니다."),
    AI_WORKER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "A003", "AI 분석 서버 응답 시간이 초과되었습니다."),

    // 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "C002", "요청 본문 형식이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 내부 에러가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
