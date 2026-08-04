package ru.yandex.practicum.kafka.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {
    private final EncoderFactory encoderFactory;

    public GeneralAvroSerializer() {
        this(EncoderFactory.get());
    }

    public GeneralAvroSerializer(EncoderFactory encoderFactory) {
        this.encoderFactory = encoderFactory;
    }

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DatumWriter<SpecificRecordBase> writer =
                    new SpecificDatumWriter<>(data.getSchema());

            BinaryEncoder encoder =
                    encoderFactory.binaryEncoder(output, null);

            writer.write(data, encoder);
            encoder.flush();

            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Не удалось сериализовать Avro-сообщение",
                    e
            );
        }
    }
}