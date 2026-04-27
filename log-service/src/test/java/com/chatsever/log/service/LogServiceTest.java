package com.chatsever.log.service;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.log.dto.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Test LogService: ghi/đọc file log, filter, phân trang. Dùng @TempDir để tạo thư mục tạm. */
class LogServiceTest {

    // Test: ghi 2 log → đọc lại phải trả đủ 2 record
    @Test
    void logAndReadBack(@TempDir Path tmp) {
        Path file = tmp.resolve("chat_log.txt");
        LogService svc = new LogService(file.toString());

        svc.log(new LogEntry(LocalDateTime.of(2026, 4, 22, 14, 35),
                "BROADCAST", "nguyen", null, "Hello"));
        svc.log(new LogEntry(LocalDateTime.of(2026, 4, 22, 14, 36),
                "PRIVATE", "nguyen", "trang", "Hi"));

        PagedResponse<LogEntry> page = svc.getHistory(0, 50, null);

        assertEquals(2, page.totalElements());
        assertEquals(0, page.page());
        assertEquals(50, page.size());
        assertEquals(1, page.totalPages());
        assertEquals("Hello", page.content().get(0).getContent());
        assertEquals("Hi", page.content().get(1).getContent());
    }

    // Test: filter theo eventType → chỉ trả record khớp
    @Test
    void filterByEventType(@TempDir Path tmp) {
        LogService svc = new LogService(tmp.resolve("log.txt").toString());
        svc.log(new LogEntry(LocalDateTime.now(), "BROADCAST", "a", null, "x"));
        svc.log(new LogEntry(LocalDateTime.now(), "PRIVATE", "a", "b", "y"));
        svc.log(new LogEntry(LocalDateTime.now(), "BROADCAST", "c", null, "z"));

        PagedResponse<LogEntry> page = svc.getHistory(0, 50, "BROADCAST");

        assertEquals(2, page.totalElements());
        page.content().forEach(e -> assertEquals("BROADCAST", e.getEventType()));
    }

    // Test: 7 record, size=3 → phải chia đúng 3 trang (3+3+1)
    @Test
    void paginationSlicesCorrectly(@TempDir Path tmp) {
        LogService svc = new LogService(tmp.resolve("log.txt").toString());
        for (int i = 0; i < 7; i++) {
            svc.log(new LogEntry(LocalDateTime.now(), "BROADCAST", "u", null, "msg" + i));
        }

        PagedResponse<LogEntry> p0 = svc.getHistory(0, 3, null);
        PagedResponse<LogEntry> p1 = svc.getHistory(1, 3, null);
        PagedResponse<LogEntry> p2 = svc.getHistory(2, 3, null);

        assertEquals(7, p0.totalElements());
        assertEquals(3, p0.totalPages());
        assertEquals(3, p0.content().size());
        assertEquals(3, p1.content().size());
        assertEquals(1, p2.content().size());
        assertEquals("msg6", p2.content().get(0).getContent());
    }

    // Test: file chưa tồn tại → trả empty, không crash
    @Test
    void emptyFileReturnsEmptyPage(@TempDir Path tmp) {
        LogService svc = new LogService(tmp.resolve("none.txt").toString());

        PagedResponse<LogEntry> page = svc.getHistory(0, 50, null);

        assertEquals(0, page.totalElements());
        assertTrue(page.content().isEmpty());
    }
}
