package me.mykindos.betterpvp.orchestration.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Instant;

public final class OrchestrationObjectMapperFactory {

    private OrchestrationObjectMapperFactory() {
    }

    public static ObjectMapper create() {
        final SimpleModule instantModule = new SimpleModule("orchestration-instant-module")
                .addSerializer(Instant.class, new InstantSerializer())
                .addDeserializer(Instant.class, new InstantDeserializer());

        return new ObjectMapper()
                .registerModule(instantModule);
    }

    private static final class InstantSerializer extends JsonSerializer<Instant> {
        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }
    }

    private static final class InstantDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final String text = p.getValueAsString();
            if (text == null || text.isBlank()) {
                return null;
            }
            return Instant.parse(text);
        }
    }
}
