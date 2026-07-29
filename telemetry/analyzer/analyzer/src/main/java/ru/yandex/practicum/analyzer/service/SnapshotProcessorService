package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.model.*;
import ru.yandex.practicum.analyzer.repository.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import net.devh.boot.grpc.client.inject.GrpcClient;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotProcessorService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public void processSnapshot(SnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.debug("Обработка снапшота для хаба: {}", hubId);

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);

        if (scenarios.isEmpty()) {
            log.debug("Нет сценариев для хаба {}", hubId);
            return;
        }

        for (Scenario scenario : scenarios) {
            try {
                processScenario(scenario, snapshot);
            } catch (Exception e) {
                log.error("Ошибка обработки сценария {} для хаба {}", scenario.getName(), hubId, e);
            }
        }
    }

    private void processScenario(Scenario scenario, SnapshotAvro snapshot) {
        Long scenarioId = scenario.getId();
        List<ScenarioCondition> scenarioConditions = scenarioConditionRepository.findByScenarioId(scenarioId);

        // Проверяем все условия сценария
        boolean allConditionsMet = scenarioConditions.stream().allMatch(sc -> {
            Condition condition = conditionRepository.findById(sc.getConditionId()).orElse(null);
            if (condition == null) return false;

            String sensorId = sc.getSensorId();
            return checkCondition(snapshot, sensorId, condition);
        });

        if (allConditionsMet) {
            log.info("Условия сценария {} выполнены для хаба {}", scenario.getName(), snapshot.getHubId());
            executeActions(scenarioId, scenario.getName(), snapshot.getHubId());
        }
    }

    private boolean checkCondition(SnapshotAvro snapshot, String sensorId, Condition condition) {
        // Ищем датчик в снапшоте
        for (DeviceStateAvro deviceState : snapshot.getDevices()) {
            if (deviceState.getId().equals(sensorId)) {
                return evaluateCondition(deviceState, condition);
            }
        }
        log.debug("Датчик {} не найден в снапшоте", sensorId);
        return false;
    }

    private boolean evaluateCondition(DeviceStateAvro deviceState, Condition condition) {
        String conditionType = condition.getType();
        String operation = condition.getOperation();
        Integer conditionValue = condition.getValue();

        // Получаем значение датчика в зависимости от типа
        Integer sensorValue = extractSensorValue(deviceState, conditionType);
        if (sensorValue == null) {
            return false;
        }

        // Сравниваем значение с условием
        return switch (operation) {
            case "EQUALS" -> sensorValue.equals(conditionValue);
            case "GREATER_THAN" -> sensorValue > conditionValue;
            case "LOWER_THAN" -> sensorValue < conditionValue;
            default -> {
                log.warn("Неизвестная операция: {}", operation);
                yield false;
            }
        };
    }

    private Integer extractSensorValue(DeviceStateAvro deviceState, String conditionType) {
        Object payload = deviceState.getPayload();

        return switch (conditionType) {
            case "TEMPERATURE" -> {
                if (payload instanceof TemperatureSensorAvro temp) {
                    yield temp.getTemperatureC();
                } else if (payload instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }
                yield null;
            }
            case "HUMIDITY" -> {
                if (payload instanceof ClimateSensorAvro climate) {
                    yield climate.getHumidity();
                }
                yield null;
            }
            case "CO2LEVEL" -> {
                if (payload instanceof ClimateSensorAvro climate) {
                    yield climate.getCo2Level();
                }
                yield null;
            }
            case "LUMINOSITY" -> {
                if (payload instanceof LightSensorAvro light) {
                    yield light.getLuminosity();
                }
                yield null;
            }
            case "MOTION" -> {
                if (payload instanceof MotionSensorAvro motion) {
                    yield motion.getMotion() ? 1 : 0;
                }
                yield null;
            }
            case "SWITCH" -> {
                if (payload instanceof SwitchSensorAvro switchSensor) {
                    yield switchSensor.getState() ? 1 : 0;
                }
                yield null;
            }
            default -> {
                log.warn("Неизвестный тип условия: {}", conditionType);
                yield null;
            }
        };
    }

    private void executeActions(Long scenarioId, String scenarioName, String hubId) {
        List<ScenarioAction> scenarioActions = scenarioActionRepository.findByScenarioId(scenarioId);

        for (ScenarioAction scenarioAction : scenarioActions) {
            Action action = actionRepository.findById(scenarioAction.getActionId()).orElse(null);
            if (action == null) continue;

            try {
                DeviceActionProto actionProto = DeviceActionProto.newBuilder()
                        .setSensorId(scenarioAction.getSensorId())
                        .setType(ActionTypeProto.valueOf(action.getType()))
                        .setValue(action.getValue() != null ? action.getValue() : 0)
                        .build();

                DeviceActionRequest request = DeviceActionRequest.newBuilder()
                        .setHubId(hubId)
                        .setScenarioName(scenarioName)
                        .setAction(actionProto)
                        .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(Instant.now().getEpochSecond())
                                .setNanos(Instant.now().getNano())
                                .build())
                        .build();

                hubRouterClient.handleDeviceAction(request);
                log.info("Отправлена команда для датчика {} в хабе {}",
                        scenarioAction.getSensorId(), hubId);
            } catch (Exception e) {
                log.error("Ошибка отправки команды для датчика {}", scenarioAction.getSensorId(), e);
            }
        }
    }
}