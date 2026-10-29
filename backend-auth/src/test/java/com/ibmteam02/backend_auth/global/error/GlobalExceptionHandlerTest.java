package com.ibmteam02.backend_auth.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibmteam02.backend_auth.global.error.dto.ErrorResponse;
import com.ibmteam02.backend_auth.global.error.exception.CustomException;
import com.ibmteam02.backend_auth.global.error.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void CustomException은_code와_message를_올바른_필드에_담는다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleCustomException(new CustomException(ErrorCode.DUPLICATE_EMAIL));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getCode()).isEqualTo("U001");
        assertThat(response.getBody().getMessage()).isEqualTo("이미 존재하는 이메일입니다");
    }

    @Test
    void 업로드_용량_초과는_413과_F001로_응답한다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(15 * 1024 * 1024));

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody().getCode()).isEqualTo("F001");
        assertThat(response.getBody().getMessage()).contains("15MB");
    }

    @Test
    void IllegalArgumentException은_예외_메시지를_message에_담는다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("비밀번호가 일치하지 않습니다"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().getMessage()).isEqualTo("비밀번호가 일치하지 않습니다");
    }
}
