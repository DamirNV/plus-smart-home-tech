package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.dto.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.ConditionOperationProto;
import ru.yandex.practicum.grpc.telemetry.event.ConditionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import java.time.Instant;
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
                builder.setPayload(DeviceAddedEventAvro.newBuilder()
                        .setId(deviceAdded.getId())
                        .setType(mapDeviceType(deviceAdded.getDeviceType()))
                        .build());
                break;

            case DEVICE_REMOVED:
                DeviceRemovedEvent deviceRemoved = (DeviceRemovedEvent) event;
                builder.setPayload(DeviceRemovedEventAvro.newBuilder()
                        .setId(deviceRemoved.getId())
                        .build());
                break;

            case SCENARIO_ADDED:
                ScenarioAddedEvent scenarioAdded = (ScenarioAddedEvent) event;
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
                builder.setPayload(ScenarioRemovedEventAvro.newBuilder()
                        .setName(scenarioRemoved.getName())
                        .build());
                break;

            default:
                throw new IllegalArgumentException("Неизвестный тип события: " + event.getType());
        }

        return builder.build();
    }

    private DeviceTypeAvro mapDeviceType(DeviceType deviceType) {
        switch (deviceType) {
            case MOTION_SENSOR:
                return DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR:
                return DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR:
                return DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR:
                return DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR:
                return DeviceTypeAvro.SWITCH_SENSOR;
            default:
                throw new IllegalArgumentException("Неизвестный тип устройства: " + deviceType);
        }
    }

    private ScenarioConditionAvro mapCondition(ScenarioCondition condition) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder();
        builder.setSensorId(condition.getSensorId());
        builder.setType(mapConditionType(condition.getType()));
        builder.setOperation(mapConditionOperation(condition.getOperation()));

        Object value = condition.getValue();
        if (value instanceof Integer) {
            builder.setValue((Integer) value);
        } else if (value instanceof Boolean) {
            builder.setValue((Boolean) value);
        } else {
            builder.setValue(null);
        }

        return builder.build();
    }

    private ConditionTypeAvro mapConditionType(ConditionType conditionType) {
        switch (conditionType) {
            case MOTION:
                return ConditionTypeAvro.MOTION;
            case LUMINOSITY:
                return ConditionTypeAvro.LUMINOSITY;
            case SWITCH:
                return ConditionTypeAvro.SWITCH;
            case TEMPERATURE:
                return ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL:
                return ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY:
                return ConditionTypeAvro.HUMIDITY;
            default:
                throw new IllegalArgumentException("Неизвестный тип условия: " + conditionType);
        }
    }

    private ConditionOperationAvro mapConditionOperation(ConditionOperation operation) {
        switch (operation) {
            case EQUALS:
                return ConditionOperationAvro.EQUALS;
            case GREATER_THAN:
                return ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN:
                return ConditionOperationAvro.LOWER_THAN;
            default:
                throw new IllegalArgumentException("Неизвестная операция: " + operation);
        }
    }

    private DeviceActionAvro mapAction(DeviceAction action) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder();
        builder.setSensorId(action.getSensorId());
        builder.setType(mapActionType(action.getType()));

        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        } else {
            builder.setValue(null);
        }

        return builder.build();
    }

    private ActionTypeAvro mapActionType(ActionType actionType) {
        switch (actionType) {
            case ACTIVATE:
                return ActionTypeAvro.ACTIVATE;
            case DEACTIVATE:
                return ActionTypeAvro.DEACTIVATE;
            case INVERSE:
                return ActionTypeAvro.INVERSE;
            case SET_VALUE:
                return ActionTypeAvro.SET_VALUE;
            default:
                throw new IllegalArgumentException("Неизвестный тип действия: " + actionType);
        }
    }

    public HubEventAvro toAvro(HubEventProto event) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder();

        builder.setHubId(event.getHubId());
        builder.setTimestamp(
                Instant.ofEpochSecond(
                        event.getTimestamp().getSeconds(),
                        event.getTimestamp().getNanos()
                ).toEpochMilli()
        );

        switch (event.getPayloadCase()) {
            case DEVICE_ADDED:
                var deviceAdded = event.getDeviceAdded();

                builder.setPayload(
                        DeviceAddedEventAvro.newBuilder()
                                .setId(deviceAdded.getId())
                                .setType(mapDeviceType(deviceAdded.getType()))
                                .build()
                );
                break;

            case DEVICE_REMOVED:
                var deviceRemoved = event.getDeviceRemoved();

                builder.setPayload(
                        DeviceRemovedEventAvro.newBuilder()
                                .setId(deviceRemoved.getId())
                                .build()
                );
                break;

            case SCENARIO_ADDED:
                var scenarioAdded = event.getScenarioAdded();

                builder.setPayload(
                        ScenarioAddedEventAvro.newBuilder()
                                .setName(scenarioAdded.getName())
                                .setConditions(
                                        scenarioAdded.getConditionList().stream()
                                                .map(this::mapCondition)
                                                .collect(Collectors.toList())
                                )
                                .setActions(
                                        scenarioAdded.getActionList().stream()
                                                .map(this::mapAction)
                                                .collect(Collectors.toList())
                                )
                                .build()
                );
                break;

            case SCENARIO_REMOVED:
                var scenarioRemoved = event.getScenarioRemoved();

                builder.setPayload(
                        ScenarioRemovedEventAvro.newBuilder()
                                .setName(scenarioRemoved.getName())
                                .build()
                );
                break;

            case PAYLOAD_NOT_SET:
                throw new IllegalArgumentException(
                        "В событии хаба отсутствует payload"
                );
        }

        return builder.build();
    }

    private DeviceTypeAvro mapDeviceType(DeviceTypeProto deviceType) {
        return switch (deviceType) {
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
            default -> throw new IllegalArgumentException(
                    "Неизвестный тип устройства: " + deviceType
            );
        };
    }

    private ScenarioConditionAvro mapCondition(ScenarioConditionProto condition) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(mapConditionType(condition.getType()))
                .setOperation(mapConditionOperation(condition.getOperation()));

        switch (condition.getValueCase()) {
            case BOOL_VALUE:
                builder.setValue(condition.getBoolValue());
                break;

            case INT_VALUE:
                builder.setValue(condition.getIntValue());
                break;

            case VALUE_NOT_SET:
                throw new IllegalArgumentException(
                        "В условии сценария отсутствует значение"
                );
        }

        return builder.build();
    }

    private ConditionTypeAvro mapConditionType(ConditionTypeProto conditionType) {
        return switch (conditionType) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
            default -> throw new IllegalArgumentException(
                    "Неизвестный тип условия: " + conditionType
            );
        };
    }

    private ConditionOperationAvro mapConditionOperation(
            ConditionOperationProto operation
    ) {
        return switch (operation) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
            default -> throw new IllegalArgumentException(
                    "Неизвестная операция условия: " + operation
            );
        };
    }

    private DeviceActionAvro mapAction(DeviceActionProto action) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(mapActionType(action.getType()));

        if (action.hasValue()) {
            builder.setValue(action.getValue());
        } else {
            builder.setValue(null);
        }

        return builder.build();
    }

    private ActionTypeAvro mapActionType(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case INVERSE -> ActionTypeAvro.INVERSE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
            default -> throw new IllegalArgumentException(
                    "Неизвестный тип действия: " + actionType
            );
        };
    }

}