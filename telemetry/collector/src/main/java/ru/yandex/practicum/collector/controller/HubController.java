package ru.yandex.practicum.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.dto.HubEvent;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.collector.service.KafkaEventProducer;

@Slf4j
@RestController
@RequestMapping("/events/hubs")
@RequiredArgsConstructor
public class HubController {

    private final KafkaEventProducer kafkaEventProducer;
    private final HubEventMapper hubEventMapper;

    @PostMapping
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        log.info("🔵 ПОЛУЧЕНО СОБЫТИЕ ОТ ХАБА: {}", event);

        try {
            var avroEvent = hubEventMapper.toAvro(event);
            log.info("🟡 СКОНВЕРТИРОВАНО В AVRO: {}", avroEvent);

            kafkaEventProducer.sendHubEvent(avroEvent);
            log.info("✅ СОБЫТИЕ УСПЕШНО ОТПРАВЛЕНО В KAFKA");
        } catch (Exception e) {
            log.error("❌ ОШИБКА ПРИ ОБРАБОТКЕ: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при обработке события хаба", e);
        }
    }
}