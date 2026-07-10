package ru.yandex.practicum.collector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioAddedEvent extends HubEvent {

    @NotBlank(message = "Название сценария не может быть пустым")
    private String name;

    @NotNull(message = "Условия не могут быть null")
    @NotEmpty(message = "Должно быть хотя бы одно условие")
    private List<ScenarioCondition> conditions;

    @NotNull(message = "Действия не могут быть null")
    @NotEmpty(message = "Должно быть хотя бы одно действие")
    private List<DeviceAction> actions;
}
