package ru.yandex.practicum.order.feign.dto;

public record ReserveRequest(
        Long productId,
        Integer quantity
) {
}