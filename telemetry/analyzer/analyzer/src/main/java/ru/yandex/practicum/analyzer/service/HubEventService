package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.*;
import ru.yandex.practicum.analyzer.repository.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.List;
import java.util.Optional;

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
            removeSensor(deviceRemoved.getId());
        } else if (payload instanceof ScenarioAddedEventAvro scenarioAdded) {
            addOrUpdateScenario(hubId, scenarioAdded);
        } else if (payload instanceof ScenarioRemovedEventAvro scenarioRemoved) {
            removeScenario(hubId, scenarioRemoved.getName());
        } else {
            log.warn("Неизвестный тип события хаба: {}", payload.getClass());
        }
    }

    private void addSensor(String hubId, DeviceAddedEventAvro deviceAdded) {
        String sensorId = deviceAdded.getId();
        Optional<Sensor> existing = sensorRepository.findById(sensorId);

        if (existing.isPresent()) {
            log.debug("Датчик {} уже существует, пропускаем", sensorId);
            return;
        }

        Sensor sensor = Sensor.builder()
                .id(sensorId)
                .hubId(hubId)
                .build();
        sensorRepository.save(sensor);
        log.info("Добавлен датчик: {} для хаба {}", sensorId, hubId);
    }

    private void removeSensor(String sensorId) {
        if (sensorRepository.existsById(sensorId)) {
            sensorRepository.deleteById(sensorId);
            log.info("Удален датчик: {}", sensorId);
        } else {
            log.debug("Датчик {} не найден, пропускаем удаление", sensorId);
        }
    }

    @Transactional
    public void addOrUpdateScenario(String hubId, ScenarioAddedEventAvro scenarioAdded) {
        String scenarioName = scenarioAdded.getName();

        Optional<Scenario> existing = scenarioRepository.findByHubIdAndName(hubId, scenarioName);

        if (existing.isPresent()) {
            log.debug("Сценарий {} уже существует для хаба {}, обновляем", scenarioName, hubId);
            Scenario scenario = existing.get();
            updateScenario(scenario, scenarioAdded);
        } else {
            log.info("Добавляем новый сценарий: {} для хаба {}", scenarioName, hubId);
            Scenario scenario = Scenario.builder()
                    .hubId(hubId)
                    .name(scenarioName)
                    .build();
            scenario = scenarioRepository.save(scenario);
            updateScenario(scenario, scenarioAdded);
        }
    }

    private void updateScenario(Scenario scenario, ScenarioAddedEventAvro scenarioAdded) {
        Long scenarioId = scenario.getId();
        String hubId = scenario.getHubId();

        // Удаляем старые условия и действия
        scenarioConditionRepository.deleteByScenarioId(scenarioId);
        scenarioActionRepository.deleteByScenarioId(scenarioId);

        // Добавляем новые условия
        for (ScenarioConditionAvro conditionAvro : scenarioAdded.getConditions()) {
            Condition condition = Condition.builder()
                    .type(conditionAvro.getType().name())
                    .operation(conditionAvro.getOperation().name())
                    .value(conditionAvro.getValue() instanceof Integer ? (Integer) conditionAvro.getValue() : null)
                    .build();
            condition = conditionRepository.save(condition);

            ScenarioCondition scenarioCondition = ScenarioCondition.builder()
                    .scenarioId(scenarioId)
                    .sensorId(conditionAvro.getSensorId())
                    .conditionId(condition.getId())
                    .build();
            scenarioConditionRepository.save(scenarioCondition);
        }

        // Добавляем новые действия
        for (DeviceActionAvro actionAvro : scenarioAdded.getActions()) {
            Action action = Action.builder()
                    .type(actionAvro.getType().name())
                    .value(actionAvro.getValue())
                    .build();
            action = actionRepository.save(action);

            ScenarioAction scenarioAction = ScenarioAction.builder()
                    .scenarioId(scenarioId)
                    .sensorId(actionAvro.getSensorId())
                    .actionId(action.getId())
                    .build();
            scenarioActionRepository.save(scenarioAction);
        }

        log.info("Сценарий {} обновлен для хаба {}", scenario.getName(), hubId);
    }

    @Transactional
    public void removeScenario(String hubId, String scenarioName) {
        Optional<Scenario> existing = scenarioRepository.findByHubIdAndName(hubId, scenarioName);

        if (existing.isPresent()) {
            Scenario scenario = existing.get();
            Long scenarioId = scenario.getId();

            scenarioConditionRepository.deleteByScenarioId(scenarioId);
            scenarioActionRepository.deleteByScenarioId(scenarioId);
            scenarioRepository.delete(scenario);

            log.info("Удален сценарий: {} для хаба {}", scenarioName, hubId);
        } else {
            log.debug("Сценарий {} не найден для хаба {}, пропускаем удаление", scenarioName, hubId);
        }
    }
}