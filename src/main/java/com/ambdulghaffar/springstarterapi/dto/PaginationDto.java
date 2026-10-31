package com.ambdulghaffar.springstarterapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationDto {

    private PaginationDto() {}

    @Schema(name = "Pagination")
    public record Pagination(

            @Schema(
                    description = "Page number (starts at 1)",
                    defaultValue = "1",
                    example = "1",
                    minimum = "1"
            )
            Integer page,

            @Schema(
                    description = "Number of elements per page",
                    defaultValue = "5",
                    example = "5",
                    minimum = "1"
            )
            Integer size,

            @Schema(
                    description = "Sorting criteria: property,direction",
                    defaultValue = "createdAt,desc",
                    example = "createdAt,desc"
            )
            String sort

    ) {

        public Pagination {
            page = page == null || page < 1 ? 1 : page;
            size = size == null || size < 1 ? 5 : size;
            sort = sort == null || sort.isBlank()
                    ? "createdAt,desc"
                    : sort;
        }

        public Pageable toPageable() {
            return PageRequest.of(
                    // page number starts from 0,but we want it to start from 1,
                    // so we add 1 to make it more user-friendly
                    page - 1,
                    size,
                    toSort()
            );
        }

        private Sort toSort() {
            String[] parts = sort.split(",");

            String property = parts[0];

            Sort.Direction direction = parts.length > 1
                    ? Sort.Direction.fromString(parts[1])
                    : Sort.Direction.ASC;

            return Sort.by(direction, property);
        }
    }
}