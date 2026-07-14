package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.dto.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEvent event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder();
        builder.setId(event.getId());
        builder.setHubId(event.getHubId());
        builder.setTimestamp(event.getTimestamp().toEpochMilli());

        switch (event.getType()) {
            case CLIMATE_SENSOR_EVENT:
                ClimateSensorEvent climateEvent = (ClimateSensorEvent) event;
                builder.setPayload(ClimateSensorAvro.newBuilder()
                        .setCo2Level(climateEvent.getCo2Level())
                        .setHumidity(climateEvent.getHumidity())
                        .setTemperatureC(climateEvent.getTemperatureC())
                        .build());
                break;

            case LIGHT_SENSOR_EVENT:
                LightSensorEvent lightEvent = (LightSensorEvent) event;
                builder.setPayload(LightSensorAvro.newBuilder()
                        .setLuminosity(lightEvent.getLuminosity())
                        .setLinkQuality(lightEvent.getLinkQuality())
                        .build());
                break;

            case MOTION_SENSOR_EVENT:
                MotionSensorEvent motionEvent = (MotionSensorEvent) event;
                builder.setPayload(MotionSensorAvro.newBuilder()
                        .setVoltage(motionEvent.getVoltage())
                        .setMotion(motionEvent.isMotion())
                        .setLinkQuality(motionEvent.getLinkQuality())
                        .build());
                break;

            case SWITCH_SENSOR_EVENT:
                SwitchSensorEvent switchEvent = (SwitchSensorEvent) event;
                builder.setPayload(SwitchSensorAvro.newBuilder()
                        .setState(switchEvent.isState())
                        .build());
                break;

            case TEMPERATURE_SENSOR_EVENT:
                TemperatureSensorEvent tempEvent = (TemperatureSensorEvent) event;
                builder.setPayload(TemperatureSensorAvro.newBuilder()
                        .setTemperatureF(tempEvent.getTemperatureF())
                        .setTemperatureC(tempEvent.getTemperatureC())
                        .build());
                break;

            default:
                throw new IllegalArgumentException("Неизвестный тип события: " + event.getType());
        }

        return builder.build();
    }
}