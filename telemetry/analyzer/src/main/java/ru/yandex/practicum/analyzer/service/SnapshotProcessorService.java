package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.Condition;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.analyzer.repository.ActionRepository;
import ru.yandex.practicum.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

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

    @Transactional(readOnly = true)
    public void processSnapshot(SensorsSnapshotAvro snapshot) {
        List<Scenario> scenarios =
                scenarioRepository.findByHubId(snapshot.getHubId());

        for (Scenario scenario : scenarios) {
            List<ScenarioCondition> conditions =
                    scenarioConditionRepository.findByScenarioId(
                            scenario.getId()
                    );

            if (conditions.isEmpty()) {
                continue;
            }

            boolean allConditionsMet = conditions.stream()
                    .allMatch(link ->
                            conditionRepository.findById(
                                            link.getConditionId()
                                    )
                                    .map(condition -> checkCondition(
                                            snapshot,
                                            link.getSensorId(),
                                            condition
                                    ))
                                    .orElse(false)
                    );

            if (allConditionsMet) {
                executeActions(
                        scenario,
                        snapshot.getHubId()
                );
            }
        }
    }

    private boolean checkCondition(
            SensorsSnapshotAvro snapshot,
            String sensorId,
            Condition condition
    ) {
        SensorStateAvro sensorState =
                snapshot.getSensorsState().get(sensorId);

        if (sensorState == null) {
            return false;
        }

        Integer actualValue =
                extractSensorValue(sensorState, condition.getType());

        Integer expectedValue = condition.getValue();

        if (actualValue == null || expectedValue == null) {
            return false;
        }

        return switch (condition.getOperation()) {
            case "EQUALS" -> actualValue.equals(expectedValue);
            case "GREATER_THAN" -> actualValue > expectedValue;
            case "LOWER_THAN" -> actualValue < expectedValue;
            default -> false;
        };
    }

    private Integer extractSensorValue(
            SensorStateAvro sensorState,
            String conditionType
    ) {
        Object data = sensorState.getData();

        return switch (conditionType) {
            case "TEMPERATURE" -> {
                if (data instanceof TemperatureSensorAvro temperature) {
                    yield temperature.getTemperatureC();
                }

                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }

                yield null;
            }

            case "HUMIDITY" ->
                    data instanceof ClimateSensorAvro climate
                            ? climate.getHumidity()
                            : null;

            case "CO2LEVEL" ->
                    data instanceof ClimateSensorAvro climate
                            ? climate.getCo2Level()
                            : null;

            case "LUMINOSITY" ->
                    data instanceof LightSensorAvro light
                            ? light.getLuminosity()
                            : null;

            case "MOTION" ->
                    data instanceof MotionSensorAvro motion
                            ? motion.getMotion() ? 1 : 0
                            : null;

            case "SWITCH" ->
                    data instanceof SwitchSensorAvro sensor
                            ? sensor.getState() ? 1 : 0
                            : null;

            default -> null;
        };
    }

    private void executeActions(Scenario scenario, String hubId) {
        List<ScenarioAction> scenarioActions =
                scenarioActionRepository.findByScenarioId(
                        scenario.getId()
                );

        for (ScenarioAction link : scenarioActions) {
            Action action = actionRepository.findById(
                            link.getActionId()
                    )
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Действие сценария не найдено"
                            )
                    );

            DeviceActionProto.Builder actionBuilder =
                    DeviceActionProto.newBuilder()
                            .setSensorId(link.getSensorId())
                            .setType(ActionTypeProto.valueOf(
                                    action.getType()
                            ));

            if (action.getValue() != null) {
                actionBuilder.setValue(action.getValue());
            }

            Instant now = Instant.now();

            DeviceActionRequest request =
                    DeviceActionRequest.newBuilder()
                            .setHubId(hubId)
                            .setScenarioName(scenario.getName())
                            .setAction(actionBuilder.build())
                            .setTimestamp(
                                    com.google.protobuf.Timestamp
                                            .newBuilder()
                                            .setSeconds(
                                                    now.getEpochSecond()
                                            )
                                            .setNanos(now.getNano())
                                            .build()
                            )
                            .build();

            hubRouterClient.handleDeviceAction(request);
        }
    }
}