package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.Condition;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.analyzer.model.Sensor;
import ru.yandex.practicum.analyzer.repository.ActionRepository;
import ru.yandex.practicum.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @Transactional
    public void processHubEvent(HubEventAvro event) {
        String hubId = event.getHubId();
        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro deviceAdded) {
            addSensor(hubId, deviceAdded);
        } else if (payload instanceof DeviceRemovedEventAvro deviceRemoved) {
            removeSensor(hubId, deviceRemoved.getId());
        } else if (payload instanceof ScenarioAddedEventAvro scenarioAdded) {
            addOrUpdateScenario(hubId, scenarioAdded);
        } else if (payload instanceof ScenarioRemovedEventAvro scenarioRemoved) {
            removeScenario(hubId, scenarioRemoved.getName());
        } else {
            throw new IllegalArgumentException(
                    "Неизвестный тип события хаба: "
                            + payload.getClass()
            );
        }
    }

    private void addSensor(
            String hubId,
            DeviceAddedEventAvro deviceAdded
    ) {
        Optional<Sensor> existing =
                sensorRepository.findById(deviceAdded.getId());

        if (existing.isPresent()) {
            if (!existing.get().getHubId().equals(hubId)) {
                throw new IllegalStateException(
                        "Датчик уже принадлежит другому хабу: "
                                + deviceAdded.getId()
                );
            }

            return;
        }

        sensorRepository.save(
                Sensor.builder()
                        .id(deviceAdded.getId())
                        .hubId(hubId)
                        .build()
        );
    }

    private void removeSensor(String hubId, String sensorId) {
        if (sensorRepository.findByIdAndHubId(
                sensorId,
                hubId
        ).isEmpty()) {
            return;
        }

        List<ScenarioCondition> conditions =
                scenarioConditionRepository.findBySensorId(sensorId);

        List<ScenarioAction> actions =
                scenarioActionRepository.findBySensorId(sensorId);

        scenarioConditionRepository.deleteBySensorId(sensorId);
        scenarioActionRepository.deleteBySensorId(sensorId);

        conditionRepository.deleteAllById(
                conditions.stream()
                        .map(ScenarioCondition::getConditionId)
                        .toList()
        );

        actionRepository.deleteAllById(
                actions.stream()
                        .map(ScenarioAction::getActionId)
                        .toList()
        );

        sensorRepository.deleteByIdAndHubId(sensorId, hubId);
    }

    private void addOrUpdateScenario(
            String hubId,
            ScenarioAddedEventAvro scenarioAdded
    ) {
        validateScenarioSensors(hubId, scenarioAdded);

        Scenario scenario =
                scenarioRepository.findByHubIdAndName(
                                hubId,
                                scenarioAdded.getName()
                        )
                        .orElseGet(() ->
                                scenarioRepository.save(
                                        Scenario.builder()
                                                .hubId(hubId)
                                                .name(
                                                        scenarioAdded
                                                                .getName()
                                                )
                                                .build()
                                )
                        );

        replaceScenarioContents(scenario, scenarioAdded);
    }

    private void validateScenarioSensors(
            String hubId,
            ScenarioAddedEventAvro scenarioAdded
    ) {
        Set<String> sensorIds = new HashSet<>();

        scenarioAdded.getConditions().forEach(
                condition -> sensorIds.add(condition.getSensorId())
        );

        scenarioAdded.getActions().forEach(
                action -> sensorIds.add(action.getSensorId())
        );

        if (sensorIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Сценарий должен содержать условия и действия"
            );
        }

        long matchingSensors =
                sensorRepository.countByIdInAndHubId(
                        sensorIds,
                        hubId
                );

        if (matchingSensors != sensorIds.size()) {
            throw new IllegalArgumentException(
                    "Сценарий содержит неизвестный датчик "
                            + "или датчик другого хаба"
            );
        }
    }

    private void replaceScenarioContents(
            Scenario scenario,
            ScenarioAddedEventAvro scenarioAdded
    ) {
        Long scenarioId = scenario.getId();

        List<ScenarioCondition> oldConditions =
                scenarioConditionRepository.findByScenarioId(
                        scenarioId
                );

        List<ScenarioAction> oldActions =
                scenarioActionRepository.findByScenarioId(
                        scenarioId
                );

        scenarioConditionRepository.deleteByScenarioId(scenarioId);
        scenarioActionRepository.deleteByScenarioId(scenarioId);

        conditionRepository.deleteAllById(
                oldConditions.stream()
                        .map(ScenarioCondition::getConditionId)
                        .toList()
        );

        actionRepository.deleteAllById(
                oldActions.stream()
                        .map(ScenarioAction::getActionId)
                        .toList()
        );

        Map<Condition, ScenarioConditionAvro> conditionsBySource =
                new LinkedHashMap<>();

        scenarioAdded.getConditions().forEach(source ->
                conditionsBySource.put(
                        Condition.builder()
                                .type(source.getType().name())
                                .operation(source.getOperation().name())
                                .value(
                                        normalizeConditionValue(
                                                source.getValue()
                                        )
                                )
                                .build(),
                        source
                )
        );

        List<Condition> conditions =
                conditionRepository.saveAll(
                        conditionsBySource.keySet()
                );

        scenarioConditionRepository.saveAll(
                conditions.stream()
                        .map(condition -> {
                            ScenarioConditionAvro source =
                                    conditionsBySource.get(condition);

                            return ScenarioCondition.builder()
                                    .scenarioId(scenarioId)
                                    .sensorId(source.getSensorId())
                                    .conditionId(condition.getId())
                                    .build();
                        })
                        .toList()
        );

        Map<Action, DeviceActionAvro> actionsBySource =
                new LinkedHashMap<>();

        scenarioAdded.getActions().forEach(source ->
                actionsBySource.put(
                        Action.builder()
                                .type(source.getType().name())
                                .value(source.getValue())
                                .build(),
                        source
                )
        );

        List<Action> actions =
                actionRepository.saveAll(
                        actionsBySource.keySet()
                );

        scenarioActionRepository.saveAll(
                actions.stream()
                        .map(action -> {
                            DeviceActionAvro source =
                                    actionsBySource.get(action);

                            return ScenarioAction.builder()
                                    .scenarioId(scenarioId)
                                    .sensorId(source.getSensorId())
                                    .actionId(action.getId())
                                    .build();
                        })
                        .toList()
        );
    }

    private Integer normalizeConditionValue(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }

        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }

        throw new IllegalArgumentException(
                "У условия отсутствует поддерживаемое значение"
        );
    }

    private void removeScenario(
            String hubId,
            String scenarioName
    ) {
        scenarioRepository.findByHubIdAndName(
                        hubId,
                        scenarioName
                )
                .ifPresent(scenario -> {
                    Long scenarioId = scenario.getId();

                    List<ScenarioCondition> conditions =
                            scenarioConditionRepository
                                    .findByScenarioId(scenarioId);

                    List<ScenarioAction> actions =
                            scenarioActionRepository
                                    .findByScenarioId(scenarioId);

                    scenarioConditionRepository
                            .deleteByScenarioId(scenarioId);

                    scenarioActionRepository
                            .deleteByScenarioId(scenarioId);

                    conditionRepository.deleteAllById(
                            conditions.stream()
                                    .map(
                                            ScenarioCondition
                                                    ::getConditionId
                                    )
                                    .toList()
                    );

                    actionRepository.deleteAllById(
                            actions.stream()
                                    .map(ScenarioAction::getActionId)
                                    .toList()
                    );

                    scenarioRepository.delete(scenario);
                });
    }
}