package ru.yandex.practicum.order.dto;

import java.math.BigDecimal;

public record PreparedOrderItem(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price
) {
}