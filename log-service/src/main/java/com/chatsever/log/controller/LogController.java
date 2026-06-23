package com.chatsever.log.controller;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.log.dto.PagedResponse;
import com.chatsever.log.service.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API xem lịch sử log.
 * GET /api/logs/history?page=0&size=50&eventType=BROADCAST
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    // Spring tự inject LogService qua constructor (không cần @Autowired)
    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * Trả lịch sử log có phân trang + filter theo eventType.
     * @param page      trang hiện tại (mặc định 0)
     * @param size      số record/trang (mặc định 50, tối đa 200)
     * @param eventType lọc theo loại event (optional)
     */
    @GetMapping("/history")
    public PagedResponse<LogEntry> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String eventType) {
        return logService.getHistory(page, size, eventType);
    }
}
