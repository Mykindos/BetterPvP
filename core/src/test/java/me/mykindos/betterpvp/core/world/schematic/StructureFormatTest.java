package me.mykindos.betterpvp.core.world.schematic;

import dev.brauw.mapper.region.Region;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureFormatTest {

    /**
     * Stands in for FastAsyncWorldEdit so the container can be tested without a server: it records only the schematic's
     * dimensions and anchor, which is all the container itself is responsible for carrying through.
     */
    private static final class StubBlockFormat implements SchematicFormat {

        @Override
        public @NotNull String id() {
            return "stub";
        }

        @Override
        public @NotNull Set<String> extensions() {
            return Set.of("stub");
        }

        @Override
        public @NotNull Schematic read(@NotNull InputStream in) throws IOException {
            final String[] parts = new String(in.readAllBytes(), StandardCharsets.UTF_8).split(",");
            return new Schematic(
                    Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]),
                    List.of());
        }

        @Override
        public void write(@NotNull OutputStream out, @NotNull Schematic schematic) throws IOException {
            final String encoded = schematic.getWidth() + "," + schematic.getHeight() + "," + schematic.getLength()
                    + "," + schematic.getAnchorX() + "," + schematic.getAnchorY() + "," + schematic.getAnchorZ();
            out.write(encoded.getBytes(StandardCharsets.UTF_8));
        }
    }

    private final StructureFormat format = new StructureFormat(new StubBlockFormat());

    private static Schematic schematicWithRegions() {
        final CapturedRegion helm = CapturedRegion.builder()
                .name("prop")
                .type(Region.RegionType.PERSPECTIVE)
                .tags(Set.of("interact:helm"))
                .points(List.of(CapturedRegion.RelativePoint.builder()
                        .x(2.5).y(3.0).z(-1.5).yaw(180f).pitch(0f).build()))
                .build();

        return new Schematic(11, 9, 21, 5, 0, 10, List.of(), List.of(helm), 90f);
    }

    private byte[] write(Schematic schematic) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(out, schematic);
        return out.toByteArray();
    }

    @Test
    @DisplayName("blocks and data-points survive a round trip through one file")
    void roundTripCarriesBothHalves() throws IOException {
        final Schematic loaded = format.read(new ByteArrayInputStream(write(schematicWithRegions())));

        assertEquals(11, loaded.getWidth());
        assertEquals(21, loaded.getLength());
        assertEquals(5, loaded.getAnchorX());
        assertEquals(10, loaded.getAnchorZ());

        assertEquals(1, loaded.getRegions().size());
        final CapturedRegion helm = loaded.getRegions().getFirst();
        assertEquals("prop", helm.getName());
        assertEquals(2.5, helm.getPoints().getFirst().getX(), 1e-9);
    }

    @Test
    @DisplayName("the capture facing survives, so a paste knows how far to rotate")
    void anchorYawSurvives() throws IOException {
        final Schematic loaded = format.read(new ByteArrayInputStream(write(schematicWithRegions())));
        assertEquals(90f, loaded.getAnchorYaw(), 0.001f);
    }

    @Test
    @DisplayName("the file contains exactly the two documented entries")
    void containerLayoutIsStable() throws IOException {
        final ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(write(schematicWithRegions())));

        boolean sawBlocks = false;
        boolean sawRegions = false;
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            sawBlocks |= StructureFormat.BLOCKS_ENTRY.equals(entry.getName());
            sawRegions |= StructureFormat.REGIONS_ENTRY.equals(entry.getName());
        }

        assertTrue(sawBlocks, "missing " + StructureFormat.BLOCKS_ENTRY);
        assertTrue(sawRegions, "missing " + StructureFormat.REGIONS_ENTRY);
    }

    @Test
    @DisplayName("a structure with no data-points is still valid, so a plain build can use the same format")
    void structureWithoutRegions() throws IOException {
        final Schematic plain = new Schematic(3, 3, 3, List.of());
        final Schematic loaded = format.read(new ByteArrayInputStream(write(plain)));

        assertTrue(loaded.getRegions().isEmpty());
        assertEquals(3, loaded.getWidth());
    }

    /**
     * Forward compatibility: a structure written by a later version carrying an extra entry must still load here rather
     * than taking the whole paste down.
     */
    @Test
    @DisplayName("an unrecognised entry is ignored rather than rejected")
    void unknownEntriesAreSkipped() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final ZipOutputStream zip = new ZipOutputStream(raw);

        zip.putNextEntry(new ZipEntry("something-from-the-future.dat"));
        zip.write("nonsense".getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry(StructureFormat.BLOCKS_ENTRY));
        new StubBlockFormat().write(zip, new Schematic(1, 1, 1, List.of()));
        zip.closeEntry();
        zip.finish();

        final Schematic loaded = format.read(new ByteArrayInputStream(raw.toByteArray()));
        assertEquals(1, loaded.getWidth());
    }

    @Test
    @DisplayName("a structure missing its blocks fails loudly rather than pasting nothing")
    void missingBlocksEntryThrows() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final ZipOutputStream zip = new ZipOutputStream(raw);
        zip.putNextEntry(new ZipEntry(StructureFormat.REGIONS_ENTRY));
        CapturedRegionCodec.write(zip, List.of(), 0f);
        zip.closeEntry();
        zip.finish();

        assertThrows(IOException.class, () -> format.read(new ByteArrayInputStream(raw.toByteArray())));
    }

    @Test
    @DisplayName("the format claims the .structure extension so the service resolves it by file name")
    void claimsExtension() {
        assertTrue(format.extensions().contains("structure"));
    }
}
