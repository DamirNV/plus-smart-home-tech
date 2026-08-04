package ru.yandex.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.service.KafkaEventProducer;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CollectorGrpcController
        extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final KafkaEventProducer kafkaEventProducer;
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;

    @Override
    public void collectSensorEvent(
            SensorEventProto request,
            StreamObserver<Empty> responseObserver
    ) {
        try {
            log.info("Получено gRPC-событие датчика: {}", request);

            var avroEvent = sensorEventMapper.toAvro(request);
            kafkaEventProducer.sendSensorEvent(avroEvent);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

            log.info("gRPC-событие датчика успешно обработано");
        } catch (Exception e) {
            log.error("Ошибка обработки gRPC-события датчика", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void collectHubEvent(
            HubEventProto request,
            StreamObserver<Empty> responseObserver
    ) {
        try {
            log.info("Получено gRPC-событие хаба: {}", request);

            var avroEvent = hubEventMapper.toAvro(request);
            kafkaEventProducer.sendHubEvent(avroEvent);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

            log.info("gRPC-событие хаба успешно обработано");
        } catch (Exception e) {
            log.error("Ошибка обработки gRPC-события хаба", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

}
