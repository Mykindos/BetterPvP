package me.mykindos.betterpvp.core.world.site.storage;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Keeping a site's record on this machine")
class LocalSiteStorageTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 7L);
    private static final SiteKey OTHER = SiteKey.of("camp", 8L);

    /** Stands in for whatever a module keeps about its own site. Lombok is not on the test processor path. */
    public static class Record {

        private String skin;
        private List<String> raised;

        public Record() {
        }

        public Record(String skin, List<String> raised) {
            this.skin = skin;
            this.raised = raised;
        }

        public String getSkin() {
            return skin;
        }

        public void setSkin(String skin) {
            this.skin = skin;
        }

        public List<String> getRaised() {
            return raised;
        }

        public void setRaised(List<String> raised) {
            this.raised = raised;
        }
    }

    @TempDir
    private Path dataFolder;

    private LocalSiteStorage storage;

    @BeforeEach
    void setUp() {
        final Core core = mock(Core.class);
        // A disabled plugin runs its tasks inline, which is what makes the futures here already complete.
        when(core.isEnabled()).thenReturn(false);
        when(core.getDataFolder()).thenReturn(dataFolder.toFile());

        storage = new LocalSiteStorage(core);
    }

    @Test
    @DisplayName("a record reads back as it was written")
    void aRecordRoundTrips() {
        storage.write(CAMP, "the camp".getBytes(StandardCharsets.UTF_8)).join();

        assertEquals("the camp", new String(storage.read(CAMP).join().orElseThrow(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("an object reads back as the type that wrote it")
    void anObjectRoundTrips() {
        storage.write(CAMP, new Record("shore", List.of("hall", "sawmill"))).join();

        final Record read = storage.read(CAMP, Record.class).join().orElseThrow();
        assertEquals("shore", read.getSkin());
        assertEquals(List.of("hall", "sawmill"), read.getRaised());
    }

    @Test
    @DisplayName("a site nothing was ever written for reads as empty rather than failing")
    void anUnwrittenSiteIsEmpty() {
        assertTrue(storage.read(CAMP).join().isEmpty());
        assertFalse(storage.exists(CAMP).join());
    }

    @Test
    @DisplayName("two owners of one site are kept apart")
    void ownersAreKeptApart() {
        storage.write(CAMP, "mine".getBytes(StandardCharsets.UTF_8)).join();
        storage.write(OTHER, "theirs".getBytes(StandardCharsets.UTF_8)).join();

        assertEquals("mine", new String(storage.read(CAMP).join().orElseThrow(), StandardCharsets.UTF_8));
        assertEquals("theirs", new String(storage.read(OTHER).join().orElseThrow(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("writing again replaces what was there")
    void writingAgainReplaces() {
        storage.write(CAMP, "before".getBytes(StandardCharsets.UTF_8)).join();
        storage.write(CAMP, "after".getBytes(StandardCharsets.UTF_8)).join();

        assertEquals("after", new String(storage.read(CAMP).join().orElseThrow(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("nothing half-written is left behind")
    void nothingHalfWrittenSurvives() throws Exception {
        storage.write(CAMP, "the camp".getBytes(StandardCharsets.UTF_8)).join();

        final File folder = dataFolder.resolve("sites").resolve("camp").toFile();
        final String[] files = folder.list();
        assertEquals(1, files == null ? 0 : files.length, "the file it was staged in should be gone");
        assertTrue(Files.exists(folder.toPath().resolve("7.json")));
    }

    @Test
    @DisplayName("a deleted record is gone")
    void aDeletedRecordIsGone() {
        storage.write(CAMP, "the camp".getBytes(StandardCharsets.UTF_8)).join();
        storage.delete(CAMP).join();

        assertEquals(Optional.empty(), storage.read(CAMP).join());
        assertFalse(storage.exists(CAMP).join());
    }

    @Test
    @DisplayName("deleting something that was never there is not an error")
    void deletingNothingIsFine() {
        storage.delete(CAMP).join();

        assertFalse(storage.exists(CAMP).join());
    }
}
