package com.chatsever.presence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.context.annotation.Import(com.chatsever.common.exception.GlobalExceptionHandler.class)
public class PresenceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PresenceApplication.class, args);
    }
}
