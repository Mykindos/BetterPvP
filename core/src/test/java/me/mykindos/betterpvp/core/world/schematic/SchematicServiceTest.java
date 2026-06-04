package me.mykindos.betterpvp.core.world.schematic;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchematicServiceTest {

    @TempDir
    java.io.File folder;

    private SchematicService service;
    private CountingFormat format;

    /**
     * A format with no Bukkit/FAWE coupling: it returns a fixed empty schematic and counts how many times it parses,
     * so we can assert caching behaviour.
     */
    private static final class CountingFormat implements SchematicFormat {
        private int reads = 0;
        private final Schematic result = new Schematic(1, 1, 1, List.of());

        @Override
        public @NotNull String id() {
            return "counting";
        }

        @Override
        public @NotNull Set<String> extensions() {
            return Set.of("test");
        }

        @Override
        public @NotNull Schematic read(@NotNull InputStream in) throws IOException {
            in.readAllBytes();
            reads++;
            return result;
        }
    }

    @BeforeEach
    void setUp() {
        service = new SchematicService(folder);
        format = new CountingFormat();
        service.register(format);
    }

    @Test
    @DisplayName("formatFor resolves by extension, case-insensitively")
    void formatForResolvesByExtension() {
        assertSame(format, service.formatFor("willow.test").orElse(null));
        assertSame(format, service.formatFor("willow.TEST").orElse(null));
        assertSame(format, service.formatFor("dir/sub/willow.test").orElse(null));
    }

    @Test
    @DisplayName("formatFor returns empty for unknown or missing extensions")
    void formatForRejectsUnknown() {
        assertTrue(service.formatFor("willow.schem").isEmpty());
        assertTrue(service.formatFor("willow").isEmpty());
        assertTrue(service.formatFor("willow.").isEmpty());
    }

    @Test
    @DisplayName("load finds a file by base name and caches the parsed schematic")
    void loadFindsByBaseNameAndCaches() throws IOException {
        Files.writeString(new java.io.File(folder, "willow_stump.test").toPath(), "bytes");

        final Optional<Schematic> first = service.load("willow_stump");
        final Optional<Schematic> second = service.load("willow_stump");

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertSame(first.get(), second.get());
        assertEquals(1, format.reads, "second load should hit the cache");
    }

    @Test
    @DisplayName("load accepts an explicit extension")
    void loadAcceptsExplicitExtension() throws IOException {
        Files.writeString(new java.io.File(folder, "rock.test").toPath(), "bytes");
        assertTrue(service.load("rock.test").isPresent());
        assertEquals(1, format.reads);
    }

    @Test
    @DisplayName("load returns empty when no matching file exists")
    void loadMissingReturnsEmpty() {
        assertTrue(service.load("does_not_exist").isEmpty());
        assertEquals(0, format.reads);
    }
}
