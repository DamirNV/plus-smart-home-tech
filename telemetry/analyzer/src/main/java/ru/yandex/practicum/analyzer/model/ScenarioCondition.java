package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_conditions")
@IdClass(ScenarioConditionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioCondition {

    @Id
    @Column(name = "scenario_id")
    private Long scenarioId;

    @Id
    @Column(name = "sensor_id")
    private String sensorId;

    @Id
    @Column(name = "condition_id")
    private Long conditionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private Scenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", insertable = false, updatable = false)
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", insertable = false, updatable = false)
    private Condition condition;
}