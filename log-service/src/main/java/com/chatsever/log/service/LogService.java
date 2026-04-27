package com.chatsever.log.service;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.log.dto.PagedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service ghi/đọc log theo format JSON Lines (mỗi dòng = 1 JSON LogEntry).
 * File mặc định: ./logs/chat_log.txt (cấu hình trong application.yml).
 * Thread-safe bằng synchronized — chỉ OK cho single instance,
 * nếu chạy nhiều instance cần chuyển sang DB.
 */
@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private final Path logFile;        // Đường dẫn file log
    private final ObjectMapper mapper; // Jackson JSON parser
    private final Object writeLock = new Object(); // Lock đồng bộ ghi file

    // Đọc path từ config, mặc định "./logs/chat_log.txt"
    public LogService(@Value("${logging.file.name:./logs/chat_log.txt}") String logFilePath) {
        this.logFile = Paths.get(logFilePath);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ensureParentDir();
    }

    // Tạo thư mục cha nếu chưa có
    private void ensureParentDir() {
        try {
            Path parent = logFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            log.warn("Không thể tạo thư mục log: {}", e.getMessage());
        }
    }

    /** Ghi 1 LogEntry vào file (append). Tự gán timestamp nếu null. */
    public void log(LogEntry entry) {
        if (entry.getTimestamp() == null) {
            entry.setTimestamp(LocalDateTime.now());
        }
        synchronized (writeLock) {
            try (BufferedWriter w = Files.newBufferedWriter(
                    logFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(mapper.writeValueAsString(entry));
                w.newLine();
            } catch (IOException e) {
                log.error("Ghi log thất bại", e);
            }
        }
    }

    /** Ghi log tin nhắn broadcast */
    public void logChat(String sender, String content) {
        log(new LogEntry(LocalDateTime.now(), "BROADCAST", sender, null, content));
    }

    /** Ghi log tin nhắn riêng tư */
    public void logPrivate(String sender, String receiver, String content) {
        log(new LogEntry(LocalDateTime.now(), "PRIVATE", sender, receiver, content));
    }

    /** Ghi log sự kiện hệ thống (LOGIN, LOGOUT, REGISTER, ...) */
    public void logSystem(String eventType, String content) {
        log(new LogEntry(LocalDateTime.now(), eventType, null, null, content));
    }

    /**
     * Đọc log + filter + phân trang.
     * size tối đa 200 để tránh response quá lớn.
     */
    public PagedResponse<LogEntry> getHistory(int page, int size, String eventType) {
        if (page < 0) page = 0;
        if (size <= 0) size = 50;
        if (size > 200) size = 200;

        List<LogEntry> all = readAll();

        // Filter theo eventType nếu có
        List<LogEntry> filtered = (eventType == null || eventType.isBlank())
                ? all
                : all.stream().filter(e -> eventType.equals(e.getEventType())).toList();

        // Cắt theo trang
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<LogEntry> slice = filtered.subList(from, to);

        return PagedResponse.of(slice, page, size, filtered.size());
    }

    /** Đọc toàn bộ file log, bỏ qua dòng lỗi format */
    private List<LogEntry> readAll() {
        if (!Files.exists(logFile)) {
            return List.of();
        }
        List<LogEntry> result = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                try {
                    result.add(mapper.readValue(line, LogEntry.class));
                } catch (IOException ex) {
                    log.warn("Bỏ qua dòng log lỗi format: {}", line);
                }
            }
        } catch (IOException e) {
            log.error("Đọc file log thất bại", e);
        }
        return result;
    }
}
