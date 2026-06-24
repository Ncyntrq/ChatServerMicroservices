package com.chatsever.log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point của log-service — chạy trên port 8084 */
@SpringBootApplication
@org.springframework.context.annotation.Import(com.chatsever.common.exception.GlobalExceptionHandler.class)
public class LogApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogApplication.class, args);
    }
}
