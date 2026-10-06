package me.mykindos.betterpvp.champions.champions.builds.screen;

import me.mykindos.betterpvp.core.menu.dialog.DialogSessions;
import me.mykindos.betterpvp.core.menu.dialog.screen.GuiRegistry;
import me.mykindos.betterpvp.core.menu.dialog.screen.GuiScreens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Skill menu screen files")
class SkillScreenFilesTest {

    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    @DisplayName("the class, editor and rename screens load and validate")
    void screensValidate() throws IOException {
        final GuiScreens screens = screens();

        assertNotNull(screens.definition("champions:classes"));
        assertNotNull(screens.definition("champions:build_editor"));
        assertNotNull(screens.definition("champions:build_rename"));
        assertEquals(List.of(), screens.validate());
    }

    @Test
    @DisplayName("bound portraits and skill icons are declared")
    void boundSpritesDeclared() throws IOException {
        final GuiScreens screens = screens();

        assertTrue(screens.hasSprite("champions", "menu/gui/classes/skills/sword_placeholder", 16, 16));
        assertTrue(screens.hasSprite("champions", "menu/gui/classes/skills/global_passive_placeholder", 16, 16));
    }

    /** Core loads first, as on the server, since the class tabs size to core's role names. */
    private static GuiScreens screens() throws IOException {
        final GuiScreens screens = new GuiScreens(new GuiRegistry(), new DialogSessions(null, null));
        screens.load("core", GuiScreens.class);
        screens.load("champions", files());
        return screens;
    }

    private static Map<String, String> files() throws IOException {
        final Map<String, String> files = new HashMap<>();
        for (String folder : List.of("gui", "translations")) {
            try (Stream<Path> paths = Files.walk(RESOURCES.resolve(folder))) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    files.put(RESOURCES.relativize(path).toString().replace('\\', '/'), Files.readString(path));
                }
            }
        }
        return files;
    }
}
