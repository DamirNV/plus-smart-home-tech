package ru.yandex.practicum.collector.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class TemperatureSensorEvent extends SensorEvent {

    @Min(value = -100, message = "Температура не может быть ниже -100°C")
    private int temperatureC;

    @Min(value = -148, message = "Температура в Фаренгейтах не может быть ниже -148°F")
    private int temperatureF;
}
