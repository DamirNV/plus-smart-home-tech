package ru.yandex.practicum.collector.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class MotionSensorEvent extends SensorEvent {

    @Min(value = 0, message = "Качество связи не может быть отрицательным")
    private int linkQuality;

    private boolean motion;

    @Min(value = 0, message = "Напряжение не может быть отрицательным")
    private int voltage;
}
