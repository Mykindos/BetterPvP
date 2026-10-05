package me.mykindos.betterpvp.core.utilities.model;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.SpriteObjectContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("ChatIcon")
class ChatIconTest {

    private static final Map<ChatIcon, Key> EXPECTED_SPRITES = Map.of(
            ChatIcon.NEWS, Key.key("betterpvp", "icon/chat/bell"),
            ChatIcon.PROBLEM, Key.key("betterpvp", "icon/chat/exclamation_mark"),
            ChatIcon.TIP, Key.key("betterpvp", "icon/chat/info"));

    private static SpriteObjectContents leadingSprite(ChatIcon icon) {
        final Component line = icon.line(Component.text("message"));
        final Component first = line.children().isEmpty() ? line : line.children().getFirst();
        final ObjectComponent object = assertInstanceOf(ObjectComponent.class, first);
        return assertInstanceOf(SpriteObjectContents.class, object.contents());
    }

    @ParameterizedTest
    @EnumSource(ChatIcon.class)
    @DisplayName("AC1: every icon leads its line with a gui atlas sprite")
    void ac1_iconsUseGuiAtlas(ChatIcon icon) {
        final SpriteObjectContents sprite = leadingSprite(icon);
        assertEquals(Key.key("minecraft", "gui"), sprite.atlas());
        assertEquals(EXPECTED_SPRITES.get(icon), sprite.sprite());
    }

    @ParameterizedTest
    @EnumSource(ChatIcon.class)
    @DisplayName("AC2: every icon's texture path is where the gui atlas reads its sprite")
    void ac2_texturePathMatchesGuiSprite(ChatIcon icon) {
        assertEquals(guiSpritePath(leadingSprite(icon).sprite()), icon.texturePath());
    }

    @Test
    @DisplayName("AC2: every icon's texture exists in the resource pack")
    void ac2_texturesExistInPack() {
        final Path pack = findPack();
        assumeTrue(pack != null, "resource pack not found, set BETTERPVP_PACK_DIR");
        for (ChatIcon icon : ChatIcon.values()) {
            final Path texture = pack.resolve(guiSpritePath(leadingSprite(icon).sprite()));
            assertTrue(Files.isRegularFile(texture), icon + " has no texture at " + texture);
        }
    }

    private static String guiSpritePath(Key sprite) {
        return "assets/" + sprite.namespace() + "/textures/gui/sprites/" + sprite.value() + ".png";
    }

    private static Path findPack() {
        final String configured = System.getenv("BETTERPVP_PACK_DIR");
        if (configured != null) {
            return Files.isDirectory(Path.of(configured)) ? Path.of(configured) : null;
        }
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            final Path candidate = dir.resolve("Resourcepack").resolve("pack");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
