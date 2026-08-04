package me.mykindos.betterpvp.core.world.schematic;

import dev.brauw.mapper.region.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturedRegionCodecTest {

    private static CapturedRegion helm() {
        return CapturedRegion.builder()
                .name("prop")
                .type(Region.RegionType.PERSPECTIVE)
                .tags(Set.of("id:helm", "interact:helm"))
                .points(List.of(CapturedRegion.RelativePoint.builder()
                        .x(3.5).y(2.0).z(-4.5).yaw(90f).pitch(0f).build()))
                .build();
    }

    private static CapturedRegion hull() {
        return CapturedRegion.builder()
                .name("ship")
                .type(Region.RegionType.CUBOID)
                .tags(Set.of("capacity:6"))
                .points(List.of(
                        CapturedRegion.RelativePoint.builder().x(-5).y(0).z(-10).yaw(0f).pitch(0f).build(),
                        CapturedRegion.RelativePoint.builder().x(5).y(8).z(10).yaw(0f).pitch(0f).build()))
                .build();
    }

    private static byte[] encode(List<CapturedRegion> regions, float anchorYaw) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        CapturedRegionCodec.write(out, regions, anchorYaw);
        return out.toByteArray();
    }

    private static CapturedRegionCodec.Document decode(byte[] bytes) throws IOException {
        return CapturedRegionCodec.read(new ByteArrayInputStream(bytes));
    }

    @Test
    @DisplayName("a captured region survives a write/read round trip intact")
    void roundTripPreservesEverything() throws IOException {
        final CapturedRegionCodec.Document document = decode(encode(List.of(helm(), hull()), 180f));

        assertEquals(2, document.getRegions().size());
        assertEquals(180f, document.getAnchorYaw(), 0.001f);

        final CapturedRegion decodedHelm = document.getRegions().getFirst();
        assertEquals("prop", decodedHelm.getName());
        assertEquals(Region.RegionType.PERSPECTIVE, decodedHelm.getType());
        assertEquals(Set.of("id:helm", "interact:helm"), decodedHelm.getTags());
        assertEquals(1, decodedHelm.getPoints().size());
        assertEquals(3.5, decodedHelm.getPoints().getFirst().getX(), 1e-9);
        assertEquals(-4.5, decodedHelm.getPoints().getFirst().getZ(), 1e-9);
        assertEquals(90f, decodedHelm.getPoints().getFirst().getYaw(), 0.001f);
    }

    @Test
    @DisplayName("multi-point regions keep their point order, so a path is not reversed by a save")
    void pointOrderIsStable() throws IOException {
        final CapturedRegion path = CapturedRegion.builder()
                .name("npc_route")
                .type(Region.RegionType.PATH)
                .tags(Set.of())
                .points(List.of(
                        CapturedRegion.RelativePoint.builder().x(1).y(0).z(0).yaw(0f).pitch(0f).build(),
                        CapturedRegion.RelativePoint.builder().x(2).y(0).z(0).yaw(0f).pitch(0f).build(),
                        CapturedRegion.RelativePoint.builder().x(3).y(0).z(0).yaw(0f).pitch(0f).build()))
                .build();

        final List<CapturedRegion.RelativePoint> points = decode(encode(List.of(path), 0f))
                .getRegions().getFirst().getPoints();

        assertEquals(1, points.get(0).getX(), 1e-9);
        assertEquals(2, points.get(1).getX(), 1e-9);
        assertEquals(3, points.get(2).getX(), 1e-9);
    }

    @Test
    @DisplayName("a structure with no data-points writes an empty list rather than failing")
    void emptyRegionsRoundTrip() throws IOException {
        final CapturedRegionCodec.Document document = decode(encode(List.of(), 0f));
        assertTrue(document.getRegions().isEmpty());
    }

    @Test
    @DisplayName("the document carries a version so a later format change can be detected")
    void documentIsVersioned() throws IOException {
        assertEquals(1, decode(encode(List.of(helm()), 0f)).getVersion());
    }

    @Test
    @DisplayName("the output is readable JSON, so a surprising paste can be diagnosed by opening the file")
    void outputIsInspectable() throws IOException {
        final String json = new String(encode(List.of(helm()), 0f), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"name\":\"prop\""), json);
        assertTrue(json.contains("PERSPECTIVE"), json);
    }
}
