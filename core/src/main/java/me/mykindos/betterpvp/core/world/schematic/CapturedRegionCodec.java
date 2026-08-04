package me.mykindos.betterpvp.core.world.schematic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.jackson.Jacksonized;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Reads and writes the data-point half of a {@code .structure} — the {@code regions.json} entry.
 * <p>
 * Plain JSON rather than the block file's NBT, because these are a handful of coordinates and tags that a human should
 * be able to open and read when a paste lands somewhere surprising.
 */
@UtilityClass
public class CapturedRegionCodec {

    // The stream belongs to the caller, matching SchematicFormat's contract. Jackson closes it by default, which would
    // slam a zip shut halfway through writing the structure's second entry.
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
            .disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

    public static void write(@NotNull OutputStream out, @NotNull List<CapturedRegion> regions, float anchorYaw)
            throws IOException {
        MAPPER.writeValue(out, new Document(1, anchorYaw, regions));
    }

    public static @NotNull Document read(@NotNull InputStream in) throws IOException {
        return MAPPER.readValue(in, new TypeReference<Document>() {
        });
    }

    /**
     * The file's top level. {@code version} exists so a later change to how points are stored can be migrated rather
     * than silently misread — a structure with the wrong coordinates would paste a ship inside a cliff.
     */
    @Value
    @Builder
    @Jacksonized
    public static class Document {

        int version;
        float anchorYaw;
        List<CapturedRegion> regions;
    }
}
