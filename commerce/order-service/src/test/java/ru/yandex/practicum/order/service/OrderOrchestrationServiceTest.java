package ru.yandex.practicum.order.service;

import feign.FeignException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.dto.ProductDto;
import ru.yandex.practicum.order.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.feign.dto.ReserveResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderOrchestrationServiceTest {

    private final OrderService orderService = mock(OrderService.class);
    private final ProductClient productClient = mock(ProductClient.class);
    private final InventoryClient inventoryClient = mock(InventoryClient.class);

    private final OrderOrchestrationService orchestrationService =
            new OrderOrchestrationService(
                    orderService,
                    productClient,
                    inventoryClient
            );

    @Test
    void shouldCreateConfirmedOrderUsingProductAndInventory() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 2)
        );

        ProductDto product = product(
                1L,
                "Умная лампа",
                "100.00",
                true
        );

        OrderDto expected = orderDto();

        when(productClient.getProductById(1L))
                .thenReturn(product);

        when(inventoryClient.reserveStock(
                new ReserveRequest(1L, 2)
        )).thenReturn(successfulReserve());

        when(orderService.saveConfirmedOrder(
                eq(request),
                anyList()
        )).thenReturn(expected);

        OrderDto actual = orchestrationService.create(request);

        assertSame(expected, actual);

        verify(productClient).getProductById(1L);

        verify(inventoryClient).reserveStock(
                new ReserveRequest(1L, 2)
        );

        verify(orderService).saveConfirmedOrder(
                eq(request),
                argThat(items ->
                        items.size() == 1
                                && items.get(0).productId().equals(1L)
                                && items.get(0).productName().equals("Умная лампа")
                                && items.get(0).quantity().equals(2)
                                && items.get(0).price().compareTo(
                                        new BigDecimal("100.00")
                                ) == 0
                )
        );

        verify(
                inventoryClient,
                never()
        ).releaseStock(new ReserveRequest(1L, 2));
    }

    @Test
    void shouldLoadRepeatedProductOnceAndReserveTotalQuantity() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(1L, 1)
        );

        when(productClient.getProductById(1L))
                .thenReturn(product(
                        1L,
                        "Умная лампа",
                        "100.00",
                        true
                ));

        when(inventoryClient.reserveStock(
                new ReserveRequest(1L, 3)
        )).thenReturn(successfulReserve());

        when(orderService.saveConfirmedOrder(
                eq(request),
                anyList()
        )).thenReturn(orderDto());

        orchestrationService.create(request);

        verify(
                productClient,
                times(1)
        ).getProductById(1L);

        verify(
                inventoryClient,
                times(1)
        ).reserveStock(
                new ReserveRequest(1L, 3)
        );
    }

    @Test
    void shouldRejectInactiveProductBeforeReservation() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 1)
        );

        when(productClient.getProductById(1L))
                .thenReturn(product(
                        1L,
                        "Умная лампа",
                        "100.00",
                        false
                ));

        assertThrows(
                OrderProcessingException.class,
                () -> orchestrationService.create(request)
        );

        verifyNoInteractions(
                inventoryClient,
                orderService
        );
    }

    @Test
    void shouldReleasePreviousReservationWhenNextReservationFails() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1)
        );

        when(productClient.getProductById(1L))
                .thenReturn(product(
                        1L,
                        "Умная лампа",
                        "100.00",
                        true
                ));

        when(productClient.getProductById(2L))
                .thenReturn(product(
                        2L,
                        "Умный датчик",
                        "50.00",
                        true
                ));

        when(inventoryClient.reserveStock(
                new ReserveRequest(1L, 2)
        )).thenReturn(successfulReserve());

        when(inventoryClient.reserveStock(
                new ReserveRequest(2L, 1)
        )).thenReturn(new ReserveResponse(
                false,
                0,
                "Недостаточно товара"
        ));

        assertThrows(
                OrderProcessingException.class,
                () -> orchestrationService.create(request)
        );

        verify(inventoryClient).releaseStock(
                new ReserveRequest(1L, 2)
        );

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldReleaseReservationWhenSavingOrderFails() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 2)
        );

        when(productClient.getProductById(1L))
                .thenReturn(product(
                        1L,
                        "Умная лампа",
                        "100.00",
                        true
                ));

        when(inventoryClient.reserveStock(
                new ReserveRequest(1L, 2)
        )).thenReturn(successfulReserve());

        when(orderService.saveConfirmedOrder(
                eq(request),
                anyList()
        )).thenThrow(
                new RuntimeException("Ошибка базы")
        );

        assertThrows(
                OrderProcessingException.class,
                () -> orchestrationService.create(request)
        );

        verify(inventoryClient).releaseStock(
                new ReserveRequest(1L, 2)
        );
    }

    @Test
    void shouldMapProductNotFoundToOrderProcessingException() {
        CreateOrderRequest request = request(
                new OrderItemRequest(99L, 1)
        );

        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(404);

        when(productClient.getProductById(99L))
                .thenThrow(exception);

        OrderProcessingException result = assertThrows(
                OrderProcessingException.class,
                () -> orchestrationService.create(request)
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "Товар с id=99 не найден",
                result.getMessage()
        );

        verifyNoInteractions(
                inventoryClient,
                orderService
        );
    }

    @Test
    void shouldMapInventoryNotFoundToOrderProcessingException() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 1)
        );

        when(productClient.getProductById(1L))
                .thenReturn(product(
                        1L,
                        "Умная лампа",
                        "100.00",
                        true
                ));

        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(404);

        when(inventoryClient.reserveStock(
                new ReserveRequest(1L, 1)
        )).thenThrow(exception);

        OrderProcessingException result = assertThrows(
                OrderProcessingException.class,
                () -> orchestrationService.create(request)
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "Складская запись для товара id=1 не найдена",
                result.getMessage()
        );

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldMapInventoryConflictToInsufficientStock() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 5)
        );

        when(productClient.getProductById(1L))
                .thenReturn(product(
                        1L,
                        "Умная лампа",
                        "100.00",
                        true
                ));

        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(409);

        when(inventoryClient.reserveStock(
                new ReserveRequest(1L, 5)
        )).thenThrow(exception);

        OrderProcessingException result = assertThrows(
                OrderProcessingException.class,
                () -> orchestrationService.create(request)
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "Недостаточно товара id=1 на складе",
                result.getMessage()
        );

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldSavePendingOrderWhenProductServiceUnavailable() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1)
        );

        when(productClient.getProductById(1L))
                .thenReturn(product(
                        1L,
                        "Smart lamp",
                        "100.00",
                        true
                ));

        when(productClient.getProductById(2L))
                .thenThrow(
                        new ProductServiceUnavailableException(
                                2L,
                                new RuntimeException("timeout")
                        )
                );

        OrderDto expected = pendingOrderDto();

        when(orderService.savePendingOrder(
                eq(request),
                anyList(),
                anyString()
        )).thenReturn(expected);

        OrderDto actual =
                orchestrationService.create(request);

        assertSame(expected, actual);

        verify(productClient).getProductById(1L);
        verify(productClient).getProductById(2L);

        verifyNoInteractions(inventoryClient);

        verify(orderService).savePendingOrder(
                eq(request),
                argThat(items ->
                        items.size() == 2

                                && items.get(0)
                                        .productId()
                                        .equals(1L)

                                && items.get(0)
                                        .productName()
                                        .contains("#1")

                                && items.get(0)
                                        .price()
                                        .compareTo(BigDecimal.ZERO) == 0

                                && items.get(1)
                                        .productId()
                                        .equals(2L)

                                && items.get(1)
                                        .productName()
                                        .contains("#2")

                                && items.get(1)
                                        .price()
                                        .compareTo(BigDecimal.ZERO) == 0
                ),
                argThat(details ->
                        details != null
                                && !details.isBlank()
                )
        );

        verify(
                orderService,
                never()
        ).saveConfirmedOrder(
                eq(request),
                anyList()
        );
    }
    @Test
    void shouldSavePendingOrderWhenInventoryServiceUnavailable() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 2)
        );

        when(productClient.getProductById(1L))
                .thenReturn(product(
                        1L,
                        "Smart lamp",
                        "100.00",
                        true
                ));

        when(inventoryClient.reserveStock(
                new ReserveRequest(1L, 2)
        )).thenThrow(
                new InventoryServiceUnavailableException(
                        1L,
                        new RuntimeException("connection refused")
                )
        );

        OrderDto expected = pendingOrderDto();

        when(orderService.savePendingOrder(
                eq(request),
                anyList(),
                anyString()
        )).thenReturn(expected);

        OrderDto actual =
                orchestrationService.create(request);

        assertSame(expected, actual);

        verify(orderService).savePendingOrder(
                eq(request),
                argThat(items ->
                        items.size() == 1

                                && items.get(0)
                                        .productId()
                                        .equals(1L)

                                && items.get(0)
                                        .productName()
                                        .equals("Smart lamp")

                                && items.get(0)
                                        .price()
                                        .compareTo(
                                                new BigDecimal("100.00")
                                        ) == 0
                ),
                argThat(details ->
                        details != null
                                && !details.isBlank()
                )
        );

        verify(
                orderService,
                never()
        ).saveConfirmedOrder(
                eq(request),
                anyList()
        );

        verify(
                inventoryClient,
                never()
        ).releaseStock(
                new ReserveRequest(1L, 2)
        );
    }

    @Test
    void shouldCompensateReservationBeforeSavingPendingOrder() {
        CreateOrderRequest request = request(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1)
        );

        when(productClient.getProductById(1L))
                .thenReturn(product(
                        1L,
                        "Smart lamp",
                        "100.00",
                        true
                ));

        when(productClient.getProductById(2L))
                .thenReturn(product(
                        2L,
                        "Smart sensor",
                        "50.00",
                        true
                ));

        when(inventoryClient.reserveStock(
                new ReserveRequest(1L, 2)
        )).thenReturn(successfulReserve());

        when(inventoryClient.reserveStock(
                new ReserveRequest(2L, 1)
        )).thenThrow(
                new InventoryServiceUnavailableException(
                        2L,
                        new RuntimeException("timeout")
                )
        );

        OrderDto expected = pendingOrderDto();

        when(orderService.savePendingOrder(
                eq(request),
                anyList(),
                anyString()
        )).thenReturn(expected);

        OrderDto actual =
                orchestrationService.create(request);

        assertSame(expected, actual);

        verify(inventoryClient).releaseStock(
                new ReserveRequest(1L, 2)
        );

        verify(orderService).savePendingOrder(
                eq(request),
                argThat(items ->
                        items.size() == 2
                                && items.get(0)
                                        .price()
                                        .compareTo(
                                                new BigDecimal("100.00")
                                        ) == 0
                                && items.get(1)
                                        .price()
                                        .compareTo(
                                                new BigDecimal("50.00")
                                        ) == 0
                ),
                anyString()
        );

        verify(
                orderService,
                never()
        ).saveConfirmedOrder(
                eq(request),
                anyList()
        );
    }
    private CreateOrderRequest request(
            OrderItemRequest... items
    ) {
        return new CreateOrderRequest(
                "Иван Петров",
                "ivan@example.com",
                List.of(items)
        );
    }

    private ProductDto product(
            Long id,
            String name,
            String price,
            boolean active
    ) {
        return new ProductDto(
                id,
                name,
                new BigDecimal(price),
                active
        );
    }

    private ReserveResponse successfulReserve() {
        return new ReserveResponse(
                true,
                10,
                "Товар успешно зарезервирован"
        );
    }

    private OrderDto pendingOrderDto() {
        return new OrderDto(
                2L,
                "Ivan Petrov",
                "ivan@example.com",
                "PENDING_CONFIRMATION",
                BigDecimal.ZERO,
                "Requires manual confirmation",
                LocalDateTime.now(),
                List.of()
        );
    }
    private OrderDto orderDto() {
        return new OrderDto(
                1L,
                "Иван Петров",
                "ivan@example.com",
                "CONFIRMED",
                new BigDecimal("200.00"),
                null,
                LocalDateTime.now(),
                List.of()
        );
    }
}