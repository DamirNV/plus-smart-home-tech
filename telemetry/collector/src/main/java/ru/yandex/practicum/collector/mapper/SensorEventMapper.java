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
                ClimateSensorAvro climateAvro = ClimateSensorAvro.newBuilder()
                        .setTemperatureC(climateEvent.getTemperatureC())
                        .setHumidity(climateEvent.getHumidity())
                        .setCo2Level(climateEvent.getCo2Level())
                        .build();
                builder.setPayload(climateAvro);
                break;

            case LIGHT_SENSOR_EVENT:
                LightSensorEvent lightEvent = (LightSensorEvent) event;
                LightSensorAvro lightAvro = LightSensorAvro.newBuilder()
                        .setLinkQuality(lightEvent.getLinkQuality())
                        .setLuminosity(lightEvent.getLuminosity())
                        .build();
                builder.setPayload(lightAvro);
                break;

            case MOTION_SENSOR_EVENT:
                MotionSensorEvent motionEvent = (MotionSensorEvent) event;
                MotionSensorAvro motionAvro = MotionSensorAvro.newBuilder()
                        .setLinkQuality(motionEvent.getLinkQuality())
                        .setMotion(motionEvent.isMotion())
                        .setVoltage(motionEvent.getVoltage())
                        .build();
                builder.setPayload(motionAvro);
                break;

            case SWITCH_SENSOR_EVENT:
                SwitchSensorEvent switchEvent = (SwitchSensorEvent) event;
                SwitchSensorAvro switchAvro = SwitchSensorAvro.newBuilder()
                        .setState(switchEvent.isState())
                        .build();
                builder.setPayload(switchAvro);
                break;

            case TEMPERATURE_SENSOR_EVENT:
                TemperatureSensorEvent tempEvent = (TemperatureSensorEvent) event;
                TemperatureSensorAvro tempAvro = TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(tempEvent.getTemperatureC())
                        .setTemperatureF(tempEvent.getTemperatureF())
                        .build();
                builder.setPayload(tempAvro);
                break;

            default:
                throw new IllegalArgumentException("Неизвестный тип события: " + event.getType());
        }

        return builder.build();
    }
}
