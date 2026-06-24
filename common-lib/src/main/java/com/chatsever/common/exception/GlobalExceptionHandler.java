package com.chatsever.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler tập trung cho toàn bộ các microservices.
 * Tuân thủ chuẩn RFC 7807 (Problem Details for HTTP APIs).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.chatserver.com/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntime(RuntimeException ex) {
        String exName = ex.getClass().getName();
        if (exName.contains("CallNotPermittedException")) {
            log.warn("Circuit Breaker is OPEN: {}", ex.getMessage());
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ hiện đang quá tải hoặc không khả dụng. Vui lòng thử lại sau.");
            problemDetail.setTitle("Service Unavailable");
            problemDetail.setType(URI.create("https://api.chatserver.com/errors/service-unavailable"));
            problemDetail.setProperty("timestamp", Instant.now());
            return problemDetail;
        }
        if (exName.contains("StatusRuntimeException")) {
            log.warn("gRPC Call Failed: {}", ex.getMessage());
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Không thể kết nối đến dịch vụ nội bộ (gRPC).");
            problemDetail.setTitle("gRPC Error");
            problemDetail.setType(URI.create("https://api.chatserver.com/errors/grpc-error"));
            problemDetail.setProperty("timestamp", Instant.now());
            return problemDetail;
        }

        log.warn("Runtime exception: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Runtime Error");
        problemDetail.setType(URI.create("https://api.chatserver.com/errors/runtime-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unexpected error: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.chatserver.com/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
