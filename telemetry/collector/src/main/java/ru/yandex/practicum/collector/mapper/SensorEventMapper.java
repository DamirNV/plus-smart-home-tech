package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.dto.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import java.time.Instant;

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
                        .setTemperatureC(climateEvent.getTemperatureC())
                        .setHumidity(climateEvent.getHumidity())
                        .setCo2Level(climateEvent.getCo2Level())
                        .build());
                break;

            case LIGHT_SENSOR_EVENT:
                LightSensorEvent lightEvent = (LightSensorEvent) event;
                builder.setPayload(LightSensorAvro.newBuilder()
                        .setLinkQuality(lightEvent.getLinkQuality())
                        .setLuminosity(lightEvent.getLuminosity())
                        .build());
                break;

            case MOTION_SENSOR_EVENT:
                MotionSensorEvent motionEvent = (MotionSensorEvent) event;
                builder.setPayload(MotionSensorAvro.newBuilder()
                        .setLinkQuality(motionEvent.getLinkQuality())
                        .setMotion(motionEvent.isMotion())
                        .setVoltage(motionEvent.getVoltage())
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
                        .setTemperatureC(tempEvent.getTemperatureC())
                        .setTemperatureF(tempEvent.getTemperatureF())
                        .build());
                break;

            default:
                throw new IllegalArgumentException("Неизвестный тип события: " + event.getType());
        }

        return builder.build();
    }

    public SensorEventAvro toAvro(SensorEventProto event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder();

        builder.setId(event.getId());
        builder.setHubId(event.getHubId());

        builder.setTimestamp(
                Instant.ofEpochSecond(
                        event.getTimestamp().getSeconds(),
                        event.getTimestamp().getNanos()
                ).toEpochMilli()
        );

        switch (event.getPayloadCase()) {
            case CLIMATE_SENSOR:
                var climateEvent = event.getClimateSensor();

                builder.setPayload(
                        ClimateSensorAvro.newBuilder()
                                .setTemperatureC(climateEvent.getTemperatureC())
                                .setHumidity(climateEvent.getHumidity())
                                .setCo2Level(climateEvent.getCo2Level())
                                .build()
                );
                break;

            case LIGHT_SENSOR:
                var lightEvent = event.getLightSensor();

                builder.setPayload(
                        LightSensorAvro.newBuilder()
                                .setLinkQuality(lightEvent.getLinkQuality())
                                .setLuminosity(lightEvent.getLuminosity())
                                .build()
                );
                break;

            case MOTION_SENSOR:
                var motionEvent = event.getMotionSensor();

                builder.setPayload(
                        MotionSensorAvro.newBuilder()
                                .setLinkQuality(motionEvent.getLinkQuality())
                                .setMotion(motionEvent.getMotion())
                                .setVoltage(motionEvent.getVoltage())
                                .build()
                );
                break;

            case SWITCH_SENSOR:
                var switchEvent = event.getSwitchSensor();

                builder.setPayload(
                        SwitchSensorAvro.newBuilder()
                                .setState(switchEvent.getState())
                                .build()
                );
                break;

            case TEMPERATURE_SENSOR:
                var temperatureEvent = event.getTemperatureSensor();

                builder.setPayload(
                        TemperatureSensorAvro.newBuilder()
                                .setTemperatureC(temperatureEvent.getTemperatureC())
                                .setTemperatureF(temperatureEvent.getTemperatureF())
                                .build()
                );
                break;

            case PAYLOAD_NOT_SET:
                throw new IllegalArgumentException(
                        "В событии датчика отсутствует payload"
                );
        }

        return builder.build();
    }

}