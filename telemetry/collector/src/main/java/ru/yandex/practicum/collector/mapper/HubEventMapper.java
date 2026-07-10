package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.dto.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.stream.Collectors;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEvent event) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder();
        builder.setHubId(event.getHubId());
        builder.setTimestamp(event.getTimestamp().toEpochMilli());

        switch (event.getType()) {
            case DEVICE_ADDED:
                DeviceAddedEvent deviceAdded = (DeviceAddedEvent) event;
                builder.setEventType(HubEventTypeAvro.DEVICE_ADDED);
                builder.setPayload(DeviceAddedEventAvro.newBuilder()
                        .setId(deviceAdded.getId())
                        .setType(mapDeviceType(deviceAdded.getDeviceType()))
                        .build());
                break;

            case DEVICE_REMOVED:
                DeviceRemovedEvent deviceRemoved = (DeviceRemovedEvent) event;
                builder.setEventType(HubEventTypeAvro.DEVICE_REMOVED);
                builder.setPayload(DeviceRemovedEventAvro.newBuilder()
                        .setId(deviceRemoved.getId())
                        .build());
                break;

            case SCENARIO_ADDED:
                ScenarioAddedEvent scenarioAdded = (ScenarioAddedEvent) event;
                builder.setEventType(HubEventTypeAvro.SCENARIO_ADDED);
                builder.setPayload(ScenarioAddedEventAvro.newBuilder()
                        .setName(scenarioAdded.getName())
                        .setConditions(
                                scenarioAdded.getConditions().stream()
                                        .map(this::mapCondition)
                                        .collect(Collectors.toList())
                        )
                        .setActions(
                                scenarioAdded.getActions().stream()
                                        .map(this::mapAction)
                                        .collect(Collectors.toList())
                        )
                        .build());
                break;

            case SCENARIO_REMOVED:
                ScenarioRemovedEvent scenarioRemoved = (ScenarioRemovedEvent) event;
                builder.setEventType(HubEventTypeAvro.SCENARIO_REMOVED);
                builder.setPayload(ScenarioRemovedEventAvro.newBuilder()
                        .setName(scenarioRemoved.getName())
                        .build());
                break;

            default:
                throw new IllegalArgumentException("Неизвестный тип события: " + event.getType());
        }

        return builder.build();
    }

    // Остальные методы (mapDeviceType, mapCondition, mapAction и т.д.) остаются без изменений
    // ...
}