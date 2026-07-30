package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_actions")
@IdClass(ScenarioActionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioAction {

    @Id
    @Column(name = "scenario_id")
    private Long scenarioId;

    @Id
    @Column(name = "sensor_id")
    private String sensorId;

    @Id
    @Column(name = "action_id")
    private Long actionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private Scenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", insertable = false, updatable = false)
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", insertable = false, updatable = false)
    private Action action;
}