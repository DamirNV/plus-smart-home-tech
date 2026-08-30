package ru.yandex.practicum.order.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.PreparedOrderItem;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.dto.ProductDto;
import ru.yandex.practicum.order.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.feign.dto.ReserveResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderOrchestrationService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderOrchestrationService.class);

    private static final String PRODUCT_DEGRADED_DETAILS =
            "Каталог временно недоступен. Заказ требует ручной проверки.";

    private static final String INVENTORY_DEGRADED_DETAILS =
            "Склад временно недоступен. Резервирование требует ручной проверки.";

    private final OrderService orderService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public OrderOrchestrationService(
            OrderService orderService,
            ProductClient productClient,
            InventoryClient inventoryClient
    ) {
        this.orderService = orderService;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
    }

    public OrderDto create(CreateOrderRequest request) {
        Map<Long, ProductDto> products = new LinkedHashMap<>();

        try {
            loadProducts(request, products);
        } catch (ProductServiceUnavailableException exception) {
            log.warn(
                    "Заказ переводится в PENDING_CONFIRMATION: product-service недоступен",
                    exception
            );

            return savePendingOrder(
                    request,
                    preparePendingItems(request, products),
                    PRODUCT_DEGRADED_DETAILS
            );
        }

        Map<Long, Integer> quantities =
                aggregateQuantities(request);

        List<ReserveRequest> successfulReservations =
                new ArrayList<>();

        try {
            reserveProducts(
                    quantities,
                    successfulReservations
            );

            return orderService.saveConfirmedOrder(
                    request,
                    prepareItems(request, products)
            );

        } catch (InventoryServiceUnavailableException exception) {
            compensate(successfulReservations);

            log.warn(
                    "Заказ переводится в PENDING_CONFIRMATION: inventory-service недоступен",
                    exception
            );

            return savePendingOrder(
                    request,
                    prepareItems(request, products),
                    INVENTORY_DEGRADED_DETAILS
            );

        } catch (OrderProcessingException exception) {
            compensate(successfulReservations);
            throw exception;

        } catch (RuntimeException exception) {
            compensate(successfulReservations);

            throw new OrderProcessingException(
                    "Не удалось оформить заказ",
                    exception
            );
        }
    }

    private OrderDto savePendingOrder(
            CreateOrderRequest request,
            List<PreparedOrderItem> preparedItems,
            String statusDetails
    ) {
        try {
            return orderService.savePendingOrder(
                    request,
                    preparedItems,
                    statusDetails
            );
        } catch (RuntimeException exception) {
            throw new OrderProcessingException(
                    "Не удалось сохранить заказ, ожидающий подтверждения",
                    exception
            );
        }
    }

    private void loadProducts(
            CreateOrderRequest request,
            Map<Long, ProductDto> products
    ) {
        for (OrderItemRequest item : request.items()) {
            if (products.containsKey(item.productId())) {
                continue;
            }

            ProductDto product =
                    getProduct(item.productId());

            if (!Boolean.TRUE.equals(product.active())) {
                throw new OrderProcessingException(
                        "Товар с id=" + item.productId()
                                + " снят с продажи"
                );
            }

            products.put(
                    item.productId(),
                    product
            );
        }
    }

    private ProductDto getProduct(Long productId) {
        try {
            return productClient.getProductById(productId);

        } catch (ProductServiceUnavailableException exception) {
            throw exception;

        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new OrderProcessingException(
                        "Товар с id=" + productId
                                + " не найден"
                );
            }

            throw new OrderProcessingException(
                    "Не удалось получить данные товара id="
                            + productId
            );
        }
    }

    private Map<Long, Integer> aggregateQuantities(
            CreateOrderRequest request
    ) {
        Map<Long, Integer> quantities =
                new LinkedHashMap<>();

        for (OrderItemRequest item : request.items()) {
            quantities.merge(
                    item.productId(),
                    item.quantity(),
                    Integer::sum
            );
        }

        return quantities;
    }

    private void reserveProducts(
            Map<Long, Integer> quantities,
            List<ReserveRequest> successfulReservations
    ) {
        for (Map.Entry<Long, Integer> entry
                : quantities.entrySet()) {

            ReserveRequest reserveRequest =
                    new ReserveRequest(
                            entry.getKey(),
                            entry.getValue()
                    );

            try {
                ReserveResponse response =
                        inventoryClient.reserveStock(
                                reserveRequest
                        );

                if (!response.success()) {
                    throw new OrderProcessingException(
                            "Недостаточно товара id="
                                    + entry.getKey()
                                    + " на складе"
                    );
                }

                successfulReservations.add(
                        reserveRequest
                );

            } catch (InventoryServiceUnavailableException exception) {
                throw exception;

            } catch (OrderProcessingException exception) {
                throw exception;

            } catch (FeignException exception) {
                throw mapInventoryException(
                        exception,
                        entry.getKey()
                );
            }
        }
    }

    private OrderProcessingException mapInventoryException(
            FeignException exception,
            Long productId
    ) {
        if (exception.status() == 404) {
            return new OrderProcessingException(
                    "Складская запись для товара id="
                            + productId
                            + " не найдена"
            );
        }

        if (exception.status() == 409) {
            return new OrderProcessingException(
                    "Недостаточно товара id="
                            + productId
                            + " на складе"
            );
        }

        return new OrderProcessingException(
                "Не удалось зарезервировать товар id="
                        + productId
        );
    }

    private List<PreparedOrderItem> prepareItems(
            CreateOrderRequest request,
            Map<Long, ProductDto> products
    ) {
        return request.items()
                .stream()
                .map(item -> {
                    ProductDto product =
                            products.get(item.productId());

                    return new PreparedOrderItem(
                            product.id(),
                            product.name(),
                            item.quantity(),
                            product.price()
                    );
                })
                .toList();
    }

    private List<PreparedOrderItem> preparePendingItems(
            CreateOrderRequest request,
            Map<Long, ProductDto> products
    ) {
        return request.items()
                .stream()
                .map(item -> {
                    ProductDto product =
                            products.get(item.productId());

                    if (product != null) {
                        return new PreparedOrderItem(
                                product.id(),
                                product.name(),
                                item.quantity(),
                                product.price()
                        );
                    }

                    return new PreparedOrderItem(
                            item.productId(),
                            "Товар #" + item.productId()
                                    + " (ожидает проверки)",
                            item.quantity(),
                            BigDecimal.ZERO
                    );
                })
                .toList();
    }

    private void compensate(
            List<ReserveRequest> successfulReservations
    ) {
        for (int i =
                successfulReservations.size() - 1;
             i >= 0;
             i--) {

            ReserveRequest reservation =
                    successfulReservations.get(i);

            try {
                inventoryClient.releaseStock(
                        reservation
                );
            } catch (Exception exception) {
                log.error(
                        "Не удалось снять резерв для товара id={}, quantity={}",
                        reservation.productId(),
                        reservation.quantity(),
                        exception
                );
            }
        }
    }
}