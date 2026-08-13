package ru.yandex.practicum.inventory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.InventoryConflictException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<InventoryDto> getAll() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public InventoryDto getByProductId(Long productId) {
        return toDto(findByProductId(productId));
    }

    @Transactional
    public InventoryDto create(UpdateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new InventoryConflictException(
                    "Складская запись для товара " + request.productId() + " уже существует"
            );
        }

        Inventory inventory = new Inventory();
        inventory.setProductId(request.productId());
        inventory.setQuantity(request.quantity());
        inventory.setReservedQuantity(0);

        return toDto(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryDto update(UpdateInventoryRequest request) {
        Inventory inventory = findByProductId(request.productId());

        if (request.quantity() < inventory.getReservedQuantity()) {
            throw new InventoryConflictException(
                    "Общее количество товара не может быть меньше уже зарезервированного количества"
            );
        }

        inventory.setQuantity(request.quantity());

        return toDto(inventoryRepository.save(inventory));
    }

    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
        Inventory inventory = findByProductId(request.productId());

        int available = inventory.getAvailableQuantity();

        if (available < request.quantity()) {
            throw new InsufficientStockException(
                    "Недостаточно товара " + request.productId()
                            + ". Доступно: " + available
                            + ", запрошено: " + request.quantity()
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + request.quantity()
        );

        Inventory saved = inventoryRepository.save(inventory);

        return new ReserveResponse(
                true,
                saved.getAvailableQuantity(),
                "Товар успешно зарезервирован"
        );
    }

    private Inventory findByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Складская запись для товара " + productId + " не найдена"
                        )
                );
    }

    private InventoryDto toDto(Inventory inventory) {
        return new InventoryDto(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity()
        );
    }
}