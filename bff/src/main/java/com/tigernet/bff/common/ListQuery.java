package com.tigernet.bff.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Tìm kiếm, sắp xếp, phân trang phía BFF. Backend hiện trả cả danh sách (chưa có phân trang),
 * nên BFF làm phần này để FE nhận đúng format bảng trên Figma (6/12/20/50 dòng, sort tăng/giảm).
 */
public record ListQuery(String keyword, String sort, int page, int size) {

    public static ListQuery of(String keyword, String sort, Integer page, Integer size) {
        return new ListQuery(keyword, sort, page == null ? 1 : page, size == null ? 20 : size);
    }

    public <T> PageResponse<T> apply(List<T> items,
                                     Function<T, String> searchText,
                                     Function<String, Comparator<T>> comparatorForField) {
        List<T> result = new ArrayList<>(items);
        String needle = Json.normalize(keyword);
        if (!needle.isEmpty()) {
            result.removeIf(item -> !Json.normalize(searchText.apply(item)).contains(needle));
        }
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            Comparator<T> comparator = comparatorForField.apply(parts[0].trim());
            if (comparator != null) {
                if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                    comparator = comparator.reversed();
                }
                result.sort(comparator);
            }
        }
        return PageResponse.of(result, page, size);
    }
}
