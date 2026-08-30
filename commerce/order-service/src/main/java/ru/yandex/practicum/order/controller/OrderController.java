package ru.yandex.practicum.order.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.service.OrderOrchestrationService;
import ru.yandex.practicum.order.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderOrchestrationService orderOrchestrationService;

    public OrderController(
            OrderService orderService,
            OrderOrchestrationService orderOrchestrationService
    ) {
        this.orderService = orderService;
        this.orderOrchestrationService = orderOrchestrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderOrchestrationService.create(request);
    }

    @GetMapping("/{id}")
    public OrderDto getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping
    public List<OrderDto> getAll() {
        return orderService.getAll();
    }

    @GetMapping("/by-email")
    public List<OrderDto> getByEmail(@RequestParam String email) {
        return orderService.getByEmail(email);
    }
}