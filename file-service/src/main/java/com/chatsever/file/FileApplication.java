package com.chatsever.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point của file-service — chạy trên port 8089 */
@SpringBootApplication
@org.springframework.context.annotation.Import(com.chatsever.common.exception.GlobalExceptionHandler.class)
public class FileApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
