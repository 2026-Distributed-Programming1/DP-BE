package org.dpbe.global.dto;

import java.util.List;

public record PageResponse<T>(
        int page,
        int size,
        int total,
        List<T> items
) {
}
