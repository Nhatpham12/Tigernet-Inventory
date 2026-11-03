package com.tigernet.bff.common;

import java.util.List;

// page bắt đầu từ 1 (khớp nhãn "Trang 1 / 2" trên Figma)
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 200));
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) safeSize));
        int safePage = Math.min(Math.max(1, page), totalPages);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(List.copyOf(all.subList(from, to)), safePage, safeSize, all.size(), totalPages);
    }
}
