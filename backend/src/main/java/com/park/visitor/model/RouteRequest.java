package com.park.visitor.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 路线查询请求 */
public record RouteRequest(
        @NotBlank(message = "起点不能为空") String fromId,
        @NotBlank(message = "终点不能为空") String toId,
        @NotNull(message = "出行方式不能为空") TravelMode mode) {
}
