package core_api.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Getter
@Builder
public class ErrorResponse {
    private final int status;
    private final String code;
    private final String message;
    private final List<FieldErrorResponse> errors;

    // ErrorCode를 받아서 ResponseEntity로 반환
    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getStatus().value())
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build()
                );
    }

    public static ResponseEntity<ErrorResponse> toResponseEntity(
            ErrorCode errorCode,
            List<FieldErrorResponse> errors
    ) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getStatus().value())
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .errors(errors)
                        .build()
                );
    }


    @Getter
    @Builder
    public static class FieldErrorResponse {
        private final String field;
        private final String message;
    }
}
