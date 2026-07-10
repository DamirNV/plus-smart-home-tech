package ru.yandex.practicum.collector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ScenarioCondition {

    @NotBlank(message = "ID датчика не может быть пустым")
    private String sensorId;

    @NotNull(message = "Тип условия не может быть null")
    private ConditionType type;

    @NotNull(message = "Операция сравнения не может быть null")
    private ConditionOperation operation;

    private Object value;
}
