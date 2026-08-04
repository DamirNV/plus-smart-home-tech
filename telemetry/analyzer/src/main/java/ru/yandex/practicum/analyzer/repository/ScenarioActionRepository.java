package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.analyzer.model.ScenarioActionId;

import java.util.List;

@Repository
public interface ScenarioActionRepository
        extends JpaRepository<ScenarioAction, ScenarioActionId> {

    List<ScenarioAction> findByScenarioId(Long scenarioId);

    List<ScenarioAction> findBySensorId(String sensorId);

    void deleteByScenarioId(Long scenarioId);

    void deleteBySensorId(String sensorId);
}