package com.jvxi.unity.novel.exception;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

@RestControllerAdvice(basePackages = "com.jvxi.unity.novel")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * ApiException 是业务异常，消息可以安全返回给前端。
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException exception, ServletWebRequest request) {
        if (isSseOrCommitted(request)) {
            return ResponseEntity.status(exception.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", exception.getMessage()));
        }
        return ResponseEntity.status(exception.getStatus())
            .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", "请求参数无效。"));
    }

    /**
     * 兜底异常处理：不向客户端泄露内部异常信息，仅返回错误编号便于排查。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknownException(Exception exception, ServletWebRequest request) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] 未捕获异常: {}", errorId, exception.getMessage(), exception);

        Map<String, Object> body = Map.of(
            "message", "服务器内部错误，请稍后重试。",
            "errorId", errorId
        );

        if (isSseOrCommitted(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(body);
    }

    private boolean isSseOrCommitted(ServletWebRequest request) {
        var response = request.getResponse();
        if (response != null && response.isCommitted()) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
