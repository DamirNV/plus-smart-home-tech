package ru.yandex.practicum.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.exception.OrderProcessingException;
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