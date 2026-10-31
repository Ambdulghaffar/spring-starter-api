package com.ambdulghaffar.springstarterapi.dto.shared;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    // Function to transform payload from Page<T> to PageResponse<T>
    public static <T> PageResponse<T> from(Page<T> page) {
        // create a new PageResponse object
        return new PageResponse<>( // { content: [], page: ....,totalPages:... }
                page.getContent(),
                // page number starts from 0, so we add 1 to make it more user-friendly
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

