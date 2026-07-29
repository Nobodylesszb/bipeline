package com.pipeline.platform.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一成功响应")
public record SuccessResponse<T>(
        @Schema(description = "业务返回码。成功固定为 200", example = "200")
        int code,

        @Schema(description = "业务数据")
        T data
) {

    public static <T> SuccessResponse<T> ok(T data) {
        return new SuccessResponse<>(200, data);
    }
}
