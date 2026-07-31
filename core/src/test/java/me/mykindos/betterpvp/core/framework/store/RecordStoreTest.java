package me.mykindos.betterpvp.core.framework.store;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RecordStoreTest {

    private static final class TestRecord {
        private final String key;
        private final int value;

        private TestRecord(String key, int value) {
            this.key = key;
            this.value = value;
        }

        String getKey() {
            return key;
        }

        int getValue() {
            return value;
        }
    }

    private static final RecordCodec<TestRecord> CODEC = new RecordCodec<>() {
        @Override
        public @NotNull String key(@NotNull TestRecord record) {
            return record.getKey();
        }

        @Override
        public void write(@NotNull DataOutputStream out, @NotNull TestRecord record) throws IOException {
            out.writeUTF(record.getKey());
            out.writeInt(record.getValue());
        }

        @Override
        public @NotNull TestRecord read(@NotNull DataInputStream in) throws IOException {
            return new TestRecord(in.readUTF(), in.readInt());
        }
    };

    @Test
    @DisplayName("removeIf deletes both the map entry and the backing file")
    void removeIfDeletesBackingFiles(@TempDir File dir) {
        final RecordStore<TestRecord> store = new RecordStore<>(dir, CODEC);
        store.put(new TestRecord("alpha", 1));
        store.put(new TestRecord("beta", 2));

        final File[] beforeFiles = Objects.requireNonNull(dir.listFiles((d, name) -> name.endsWith(".bin")));
        assertEquals(2, beforeFiles.length);

        final int removed = store.removeIf(record -> record.getValue() == 1);

        assertEquals(1, removed);
        assertTrue(store.get("alpha").isEmpty());
        assertTrue(store.get("beta").isPresent());

        final File[] afterFiles = Objects.requireNonNull(dir.listFiles((d, name) -> name.endsWith(".bin")));
        assertEquals(1, afterFiles.length);
    }

    @Test
    @DisplayName("fileFor sanitises keys containing slashes, colons and spaces into safe filenames")
    void fileForSanitisesUnsafeCharacters(@TempDir File dir) {
        final RecordStore<TestRecord> store = new RecordStore<>(dir, CODEC);
        final String unsafeKey = "world/one: chunk 4,-2";

        store.put(new TestRecord(unsafeKey, 42));

        final File[] files = Objects.requireNonNull(dir.listFiles((d, name) -> name.endsWith(".bin")));
        assertEquals(1, files.length);
        assertFalse(files[0].getName().contains("/"));
        assertFalse(files[0].getName().contains(":"));
        assertFalse(files[0].getName().contains(" "));
        assertTrue(store.get(unsafeKey).isPresent());
    }

    @Test
    @DisplayName("records re-key from the deserialized record body when loaded from disk")
    void recordsReKeyOnLoad(@TempDir File dir) {
        RecordStore<TestRecord> store = new RecordStore<>(dir, CODEC);
        store.put(new TestRecord("gamma", 7));

        // A fresh store over the same directory must recover the record under its own key without an index file.
        final RecordStore<TestRecord> reloaded = new RecordStore<>(dir, CODEC);

        assertTrue(reloaded.get("gamma").isPresent());
        assertEquals(7, reloaded.get("gamma").orElseThrow().getValue());
    }
}
