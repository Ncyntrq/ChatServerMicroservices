package com.chatsever.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import com.chatsever.common.exception.GlobalExceptionHandler;


@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class MessagingApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessagingApplication.class, args);
    }

}