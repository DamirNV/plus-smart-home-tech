package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.analyzer.model.ScenarioConditionId;

import java.util.List;

@Repository
public interface ScenarioConditionRepository
        extends JpaRepository<ScenarioCondition, ScenarioConditionId> {

    List<ScenarioCondition> findByScenarioId(Long scenarioId);

    List<ScenarioCondition> findBySensorId(String sensorId);

    void deleteByScenarioId(Long scenarioId);

    void deleteBySensorId(String sensorId);
}