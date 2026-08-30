package ru.yandex.practicum.order.feign.dto;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String name,
        BigDecimal price,
        Boolean active
) {
}