package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.model.Sensor;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, String> {

    long countByIdInAndHubId(
            Collection<String> ids,
            String hubId
    );

    Optional<Sensor> findByIdAndHubId(
            String id,
            String hubId
    );

    void deleteByIdAndHubId(
            String id,
            String hubId
    );
}