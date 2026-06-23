package com.chatsever.friend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(com.chatsever.common.exception.GlobalExceptionHandler.class)
public class FriendServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FriendServiceApplication.class, args);
    }
}
