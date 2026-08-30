package ru.yandex.practicum.inventory.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.service.InventoryService;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryDto> getAll() {
        return inventoryService.getAll();
    }

    @GetMapping("/{productId}")
    public InventoryDto getByProductId(@PathVariable Long productId) {
        return inventoryService.getByProductId(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryDto create(
            @Valid @RequestBody UpdateInventoryRequest request
    ) {
        return inventoryService.create(request);
    }

    @PutMapping
    public InventoryDto update(
            @Valid @RequestBody UpdateInventoryRequest request
    ) {
        return inventoryService.update(request);
    }

    @PostMapping("/reserve")
    public ReserveResponse reserve(
            @Valid @RequestBody ReserveRequest request
    ) {
        return inventoryService.reserve(request);
    }

    @PostMapping("/release")
    public ReserveResponse release(
            @Valid @RequestBody ReserveRequest request
    ) {
        return inventoryService.release(request);
    }
}