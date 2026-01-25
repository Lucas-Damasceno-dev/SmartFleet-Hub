package io.smartfleet.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Standard API response wrapper following SmartFleet API contracts.
 * All API responses must follow this structure.
 *
 * @param <T> The type of the data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private T data;
    private Meta meta;
    
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private Integer page;
        private Integer size;
        private Long total;
    }

    public static <T> ApiResponse<T> of(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> of(T data, int page, int size, long total) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(Meta.builder()
                        .page(page)
                        .size(size)
                        .total(total)
                        .build())
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
