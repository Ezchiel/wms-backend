package vn.edu.hcmuaf.fit.wms.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Meta meta;

    @Getter
    @Setter
    @Builder
    public static class Meta {
        private String timestamp;
        private String version;

        // Pagination
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private Long totalElements;
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(null)
                .meta(Meta.builder()
                        .timestamp(Instant.now().toString())
                        .version("v1")
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .meta(Meta.builder()
                        .timestamp(Instant.now().toString())
                        .version("v1")
                        .build())
                .build();
    }

    public static <T> ApiResponse<List<T>> success(String message, Page<T> pageData) {
        return ApiResponse.<List<T>>builder()
                .success(true)
                .message(message)
                .data(pageData.getContent())
                .meta(Meta.builder()
                        .timestamp(Instant.now().toString())
                        .version("v1")
                        .page(pageData.getNumber() + 1)
                        .size(pageData.getSize())
                        .totalPages(pageData.getTotalPages())
                        .totalElements(pageData.getTotalElements())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .meta(Meta.builder()
                        .timestamp(Instant.now().toString())
                        .version("v1")
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .meta(Meta.builder()
                        .timestamp(Instant.now().toString())
                        .version("v1")
                        .build())
                .build();
    }
}
