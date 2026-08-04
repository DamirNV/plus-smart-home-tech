package ru.yandex.practicum.analyzer.serialization;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class AvroDeserializer {

    public <T extends SpecificRecordBase> T deserialize(byte[] data, Class<T> clazz) {
        try {
            T record = clazz.getDeclaredConstructor().newInstance();
            SpecificDatumReader<T> reader = new SpecificDatumReader<>(record.getSchema());
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            return reader.read(record, decoder);
        } catch (IOException | ReflectiveOperationException e) {
            log.error("Ошибка десериализации Avro-сообщения", e);
            throw new RuntimeException("Не удалось десериализовать Avro-сообщение", e);
        }
    }
}