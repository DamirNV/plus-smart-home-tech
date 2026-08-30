package ru.yandex.practicum.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderItemRequest(

        @NotNull(message = "ID товара обязателен")
        Long productId,

        @NotNull(message = "Количество обязательно")
        @Min(value = 1, message = "Количество должно быть не менее 1")
        Integer quantity
) {

    public OrderItemRequest(
            Long productId,
            String ignoredProductName,
            Integer quantity,
            BigDecimal ignoredPrice
    ) {
        this(productId, quantity);
    }
}