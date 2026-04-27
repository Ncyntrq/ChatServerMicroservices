package com.chatsever.log.controller;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.log.dto.PagedResponse;
import com.chatsever.log.service.LogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test REST API /api/logs/history bằng MockMvc.
 * LogService được mock — chỉ test controller layer.
 */
@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LogService logService;

    // Test: gọi GET /history → trả về đúng format PagedResponse
    @Test
    void historyReturnsPagedResponse() throws Exception {
        LogEntry e = new LogEntry(LocalDateTime.of(2026, 4, 22, 14, 35),
                "BROADCAST", "nguyen", null, "Hello");
        when(logService.getHistory(eq(0), eq(50), isNull()))
                .thenReturn(PagedResponse.of(List.of(e), 0, 50, 1));

        mvc.perform(get("/api/logs/history").param("page", "0").param("size", "50"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.page").value(0))
           .andExpect(jsonPath("$.size").value(50))
           .andExpect(jsonPath("$.totalElements").value(1))
           .andExpect(jsonPath("$.totalPages").value(1))
           .andExpect(jsonPath("$.content[0].sender").value("nguyen"))
           .andExpect(jsonPath("$.content[0].eventType").value("BROADCAST"));
    }

    // Test: truyền eventType=PRIVATE → filter phải hoạt động
    @Test
    void historyAcceptsEventTypeFilter() throws Exception {
        when(logService.getHistory(eq(0), eq(50), eq("PRIVATE")))
                .thenReturn(PagedResponse.of(List.of(), 0, 50, 0));

        mvc.perform(get("/api/logs/history")
                        .param("page", "0")
                        .param("size", "50")
                        .param("eventType", "PRIVATE"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.totalElements").value(0));
    }
}
