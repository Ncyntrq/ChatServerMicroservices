package com.chatsever.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Entry point của notification-service — chạy trên port 8088 */
@SpringBootApplication
@Import(com.chatsever.common.exception.GlobalExceptionHandler.class)
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
