package ru.yandex.practicum.collector.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ClimateSensorEvent extends SensorEvent {

    @Min(value = -100, message = "Температура не может быть ниже -100°C")
    private int temperatureC;

    @Min(value = 0, message = "Влажность не может быть отрицательной")
    private int humidity;

    @Min(value = 0, message = "Уровень CO2 не может быть отрицательным")
    private int co2Level;
}