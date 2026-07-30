package ru.yandex.practicum.aggregator.service;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotServiceTest {

    private final SnapshotService service = new SnapshotService();

    @Test
    void shouldIgnoreDuplicateAndOlderEvents() {
        SensorEventAvro first =
                event(Instant.parse("2026-01-01T10:00:00Z"), true);

        SensorEventAvro duplicate =
                event(Instant.parse("2026-01-01T10:01:00Z"), true);

        SensorEventAvro older =
                event(Instant.parse("2026-01-01T09:59:00Z"), false);

        SensorEventAvro changed =
                event(Instant.parse("2026-01-01T10:02:00Z"), false);

        assertThat(service.updateState(first)).isPresent();
        assertThat(service.updateState(duplicate)).isEmpty();
        assertThat(service.updateState(older)).isEmpty();
        assertThat(service.updateState(changed)).isPresent();
    }

    private SensorEventAvro event(Instant timestamp, boolean state) {
        return SensorEventAvro.newBuilder()
                .setId("switch-1")
                .setHubId("hub-1")
                .setTimestamp(timestamp)
                .setPayload(
                        SwitchSensorAvro.newBuilder()
                                .setState(state)
                                .build()
                )
                .build();
    }
}