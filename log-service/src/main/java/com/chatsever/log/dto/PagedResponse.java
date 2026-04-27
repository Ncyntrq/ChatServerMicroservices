package com.chatsever.log.dto;

import java.util.List;

/**
 * Response phân trang cho GET /api/logs/history.
 * Dùng Java Record — tự generate constructor, getter, equals, hashCode.
 */
public record PagedResponse<T>(
        List<T> content,       // Danh sách record trong trang hiện tại
        int page,              // Trang hiện tại (bắt đầu từ 0)
        int size,              // Số record tối đa mỗi trang
        long totalElements,    // Tổng số record thỏa điều kiện
        int totalPages         // Tổng số trang
) {
    /** Factory method — tự tính totalPages từ totalElements và size */
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PagedResponse<>(content, page, size, totalElements, totalPages);
    }
}
