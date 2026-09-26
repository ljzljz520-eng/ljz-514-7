package com.park.route.dto;

import com.park.route.model.TravelMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RouteRequest(
        @NotBlank(message = "起点不能为空") String from,
        @NotBlank(message = "终点不能为空") String to,
        @NotNull(message = "出行方式不能为空") TravelMode mode
) {}
