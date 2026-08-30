package ru.yandex.practicum.order.feign.dto;

public record ReserveResponse(
        boolean success,
        Integer availableQuantity,
        String message
) {
}