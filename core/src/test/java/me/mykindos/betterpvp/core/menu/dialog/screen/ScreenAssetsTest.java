package me.mykindos.betterpvp.core.menu.dialog.screen;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ScreenAssets")
class ScreenAssetsTest {

    static String resource(String path) throws IOException {
        try (InputStream input = Objects.requireNonNull(ScreenAssetsTest.class.getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("the golden screen needs exactly the expected glyphs, in code order")
    void goldenGlyphs() throws IOException {
        final ScreenDefinition screen = ScreenParser.screen("golden", "shop.json", resource("/gui-golden/shop.json"));
        final List<String> expected = new ArrayList<>();
        JsonParser.parseString(resource("/gui-golden/shop.expected.json")).getAsJsonObject()
                .getAsJsonArray("glyphs").forEach(glyph -> expected.add(glyph.getAsString()));

        final List<String> glyphs = ScreenAssets.glyphs(ScreenAssets.collect(screen,
                (definition, name) -> definition.getComponents().get(name)));

        assertEquals(expected, glyphs);
    }

    @Test
    @DisplayName("glyph codes follow the sorted glyph order from U+E000")
    void codesFollowOrder() throws IOException {
        final ScreenDefinition screen = ScreenParser.screen("golden", "shop.json", resource("/gui-golden/shop.json"));
        final AssetTable table = new AssetTable("golden", ScreenAssets.collect(screen,
                (definition, name) -> definition.getComponents().get(name)));

        assertEquals('\uE000', table.code("anim:coin_spin:16x16:8:10"));
        assertEquals('\uE001', table.code("backdrop:panel:300x160:0#0"));
        assertEquals('\uE00E', table.code("pressed:steel:100x24"));
    }

    @Test
    @DisplayName("wide art splits into equal glyphs under the font page width")
    void wideArtSplits() {
        assertEquals(2, ScreenAssets.parts("box:panel:300x40"));
        assertEquals(150, ScreenAssets.partWidth("box:panel:300x40", 1));
        assertEquals(3, ScreenAssets.parts("box:panel:513x40"));
        assertEquals(171, ScreenAssets.partWidth("box:panel:513x40", 2));
        assertEquals(1, ScreenAssets.parts("hover:rim:200x20:300:0:0"));
    }
}
