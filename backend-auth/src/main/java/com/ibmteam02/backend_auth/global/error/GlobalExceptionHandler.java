package com.ibmteam02.backend_auth.global.error;

import com.ibmteam02.backend_auth.global.error.exception.CustomException;
import com.ibmteam02.backend_auth.global.error.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import com.ibmteam02.backend_auth.global.error.dto.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.HttpStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e){
        return toResponse(e.getErrorCode());
    }

    //업로드 용량 초과 시 500이 아닌 413으로 명확히 응답
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    protected ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return toResponse(ErrorCode.FILE_SIZE_EXCEEDED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .code("BAD_REQUEST")
                        .message(e.getMessage())
                        .build());
    }

    // 생성자 인자 순서(status, message, code)와 달라 code·message가 뒤바뀌던 문제를 막기 위해 필드명으로 지정
    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getStatus())
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }
}
