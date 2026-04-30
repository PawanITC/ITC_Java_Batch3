package com.itc.funkart.kafka;



import com.itc.funkart.event.ReviewEventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AvroDeserializer {

    public ReviewEventEnvelope deserializeEnvelope(byte[] bytes) {
        try {
            SpecificDatumReader<ReviewEventEnvelope> reader =
                    new SpecificDatumReader<>(ReviewEventEnvelope.class);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize ReviewEventEnvelope", e);
        }
    }
}

