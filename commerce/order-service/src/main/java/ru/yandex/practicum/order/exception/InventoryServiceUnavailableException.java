package ru.yandex.practicum.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {

    public InventoryServiceUnavailableException(Long productId, Throwable cause) {
        super("inventory-service временно недоступен для товара id=" + productId, cause);
    }
}