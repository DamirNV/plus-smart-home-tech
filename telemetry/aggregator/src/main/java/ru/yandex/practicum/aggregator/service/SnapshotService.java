package ru.yandex.practicum.aggregator.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SnapshotService {

    private final Map<String, SensorsSnapshotAvro> snapshots =
            new ConcurrentHashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        Instant eventTimestamp =
                Instant.ofEpochMilli(event.getTimestamp());

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(
                event.getHubId(),
                hubId -> SensorsSnapshotAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(eventTimestamp)
                        .setSensorsState(new HashMap<>())
                        .build()
        );

        SensorStateAvro oldState =
                snapshot.getSensorsState().get(event.getId());

        if (oldState != null) {
            boolean eventIsNotNewer =
                    !eventTimestamp.isAfter(oldState.getTimestamp());

            boolean payloadDidNotChange =
                    oldState.getData().equals(event.getPayload());

            if (eventIsNotNewer || payloadDidNotChange) {
                return Optional.empty();
            }
        }

        Map<String, SensorStateAvro> newStates =
                new HashMap<>(snapshot.getSensorsState());

        newStates.put(
                event.getId(),
                SensorStateAvro.newBuilder()
                        .setTimestamp(eventTimestamp)
                        .setData(event.getPayload())
                        .build()
        );

        SensorsSnapshotAvro updated =
                SensorsSnapshotAvro.newBuilder(snapshot)
                        .setTimestamp(eventTimestamp)
                        .setSensorsState(newStates)
                        .build();

        snapshots.put(event.getHubId(), updated);

        return Optional.of(updated);
    }
}