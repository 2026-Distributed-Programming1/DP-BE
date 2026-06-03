package org.dpbe.global.dto;

import java.util.List;

public record ItemsResponse<T>(
        List<T> items
) {
}
