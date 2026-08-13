package ru.yandex.practicum.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderDto create(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus("CREATED");
        order.setStatusDetails(null);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest requestItem : request.items()) {
            OrderItem item = new OrderItem();

            item.setProductId(requestItem.productId());
            item.setProductName(requestItem.productName());
            item.setQuantity(requestItem.quantity());
            item.setPrice(requestItem.price());

            order.addItem(item);

            totalPrice = totalPrice.add(
                    requestItem.price()
                            .multiply(BigDecimal.valueOf(requestItem.quantity()))
            );
        }

        order.setTotalPrice(totalPrice);

        return toDto(orderRepository.save(order));
    }

    public OrderDto getById(Long id) {
        return toDto(findEntity(id));
    }

    public List<OrderDto> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<OrderDto> getByEmail(String email) {
        return orderRepository.findAllByCustomerEmail(email)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private Order findEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Заказ не найден: " + id)
                );
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems()
                .stream()
                .map(this::toItemDto)
                .toList();

        return new OrderDto(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getStatusDetails(),
                order.getCreatedAt(),
                items
        );
    }

    private OrderItemDto toItemDto(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}