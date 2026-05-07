package com.itc.funkart.outbox;

import lombok.RequiredArgsConstructor;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public void saveEventToOutbox(SpecificRecordBase avroEvent) {
        byte[] payload = serializeAvro(avroEvent);

        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(UUID.randomUUID().toString());
        outbox.setEventType(avroEvent.getSchema().getName());
        outbox.setOccurredAt(Instant.now());
        outbox.setPayload(payload);
        outbox.setProcessed(false);

        outboxRepository.save(outbox);
    }

    private byte[] serializeAvro(SpecificRecordBase record) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DatumWriter<SpecificRecordBase> writer =
                    new SpecificDatumWriter<>(record.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(record, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Avro event", e);
        }
    }
}
