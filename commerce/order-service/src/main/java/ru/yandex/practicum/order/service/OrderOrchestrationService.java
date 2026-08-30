package ru.yandex.practicum.order.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.PreparedOrderItem;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.dto.ProductDto;
import ru.yandex.practicum.order.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.feign.dto.ReserveResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderOrchestrationService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderOrchestrationService.class);

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
        Map<Long, ProductDto> products = loadProducts(request);
        Map<Long, Integer> quantities = aggregateQuantities(request);

        List<ReserveRequest> successfulReservations = new ArrayList<>();

        try {
            reserveProducts(quantities, successfulReservations);

            List<PreparedOrderItem> preparedItems =
                    prepareItems(request, products);

            return orderService.saveConfirmedOrder(
                    request,
                    preparedItems
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

    private Map<Long, ProductDto> loadProducts(
            CreateOrderRequest request
    ) {
        Map<Long, ProductDto> products = new LinkedHashMap<>();

        for (OrderItemRequest item : request.items()) {
            if (products.containsKey(item.productId())) {
                continue;
            }

            ProductDto product = getProduct(item.productId());

            if (!Boolean.TRUE.equals(product.active())) {
                throw new OrderProcessingException(
                        "Товар с id=" + item.productId()
                                + " снят с продажи"
                );
            }

            products.put(item.productId(), product);
        }

        return products;
    }

    private ProductDto getProduct(Long productId) {
        try {
            return productClient.getProductById(productId);
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new OrderProcessingException(
                        "Товар с id=" + productId + " не найден"
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
        Map<Long, Integer> quantities = new LinkedHashMap<>();

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
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ReserveRequest reserveRequest = new ReserveRequest(
                    entry.getKey(),
                    entry.getValue()
            );

            try {
                ReserveResponse response =
                        inventoryClient.reserveStock(reserveRequest);

                if (!response.success()) {
                    throw new OrderProcessingException(
                            "Недостаточно товара id="
                                    + entry.getKey()
                                    + " на складе"
                    );
                }

                successfulReservations.add(reserveRequest);
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

    private void compensate(
            List<ReserveRequest> successfulReservations
    ) {
        for (int i = successfulReservations.size() - 1; i >= 0; i--) {
            ReserveRequest reservation =
                    successfulReservations.get(i);

            try {
                inventoryClient.releaseStock(reservation);
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