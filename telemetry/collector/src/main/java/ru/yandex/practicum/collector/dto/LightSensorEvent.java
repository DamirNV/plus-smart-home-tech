package ru.yandex.practicum.collector.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class LightSensorEvent extends SensorEvent {

    @Min(value = 0, message = "Качество связи не может быть отрицательным")
    private int linkQuality;

    @Min(value = 0, message = "Освещённость не может быть отрицательной")
    private int luminosity;
}
