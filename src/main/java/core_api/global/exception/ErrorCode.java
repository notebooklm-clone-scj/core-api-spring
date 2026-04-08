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

    // Notebook 관련 에러
    NOTEBOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "해당 노트북을 찾을 수 없습니다."),

    // PDF 관련 에러
    PDF_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P001", "PDF 텍스트 추출에 실패했습니다."),

    // 파일 관련 에러
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "파일을 읽는 중 에러가 발생했습니다."),

    // 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 내부 에러가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
