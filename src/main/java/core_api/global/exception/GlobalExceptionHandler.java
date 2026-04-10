package core_api.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice // 모든 컨트롤러의 에러를 가로채는 역할
public class GlobalExceptionHandler {

    // 만들었던 CustomException이 발생할 때
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.warn("CustomException: code={}, message={}",
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage());
        return ErrorResponse.toResponseEntity(e.getErrorCode());
    }

    // 검증 실패 시 HTML 에러나 이상한 응답 대신 JSON을 받도록
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e
    ) {

        List<ErrorResponse.FieldErrorResponse> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> ErrorResponse.FieldErrorResponse.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_INPUT_VALUE, errors);
    }

    // JSON 메시지가 잘못되었을 때 대비
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleInvalidRequestBody(
            HttpMessageNotReadableException e
    ) {
        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_REQUEST_BODY);
    }

    // 예상치 못한 에러가 발생했을 때 대비
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return ErrorResponse.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }

}
