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
                DeviceAddedEventAvro deviceAddedAvro = DeviceAddedEventAvro.newBuilder()
                        .setId(deviceAdded.getId())
                        .setType(mapDeviceType(deviceAdded.getDeviceType()))
                        .build();
                builder.setPayload(deviceAddedAvro);
                break;

            case DEVICE_REMOVED:
                DeviceRemovedEvent deviceRemoved = (DeviceRemovedEvent) event;
                DeviceRemovedEventAvro deviceRemovedAvro = DeviceRemovedEventAvro.newBuilder()
                        .setId(deviceRemoved.getId())
                        .build();
                builder.setPayload(deviceRemovedAvro);
                break;

            case SCENARIO_ADDED:
                ScenarioAddedEvent scenarioAdded = (ScenarioAddedEvent) event;
                ScenarioAddedEventAvro scenarioAddedAvro = ScenarioAddedEventAvro.newBuilder()
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
                        .build();
                builder.setPayload(scenarioAddedAvro);
                break;

            case SCENARIO_REMOVED:
                ScenarioRemovedEvent scenarioRemoved = (ScenarioRemovedEvent) event;
                ScenarioRemovedEventAvro scenarioRemovedAvro = ScenarioRemovedEventAvro.newBuilder()
                        .setName(scenarioRemoved.getName())
                        .build();
                builder.setPayload(scenarioRemovedAvro);
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

        Integer value = action.getValue();
        if (value != null) {
            builder.setValue(value);
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
}