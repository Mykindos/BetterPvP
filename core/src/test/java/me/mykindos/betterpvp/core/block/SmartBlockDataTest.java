package me.mykindos.betterpvp.core.block;

import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmartBlockDataTest {

    // Test data class
    public static class TestBlockData {
        private final int level;
        private final String name;
        private final boolean active;

        public TestBlockData(int level, String name, boolean active) {
            this.level = level;
            this.name = name;
            this.active = active;
        }

        // Getters and setters
        public int getLevel() { return level; }
        public String getName() { return name; }
        public boolean isActive() { return active; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestBlockData that = (TestBlockData) obj;
            return level == that.level && active == that.active &&
                   (Objects.equals(name, that.name));
        }
    }

    // Test serializer for TestBlockData
    public static class TestBlockDataSerializer implements SmartBlockDataSerializer<TestBlockData> {
        private static final NamespacedKey LEVEL_KEY = new NamespacedKey("test", "level");
        private static final NamespacedKey NAME_KEY = new NamespacedKey("test", "name");
        private static final NamespacedKey ACTIVE_KEY = new NamespacedKey("test", "active");

        @Override
        public @NotNull NamespacedKey getKey() {
            return new NamespacedKey("test", "data");
        }

        @Override
        public @NotNull Class<TestBlockData> getType() {
            return TestBlockData.class;
        }

        @Override
        public void serialize(@NotNull TestBlockData data, @NotNull PersistentDataContainer container) {
            container.set(LEVEL_KEY, PersistentDataType.INTEGER, data.getLevel());
            container.set(NAME_KEY, PersistentDataType.STRING, data.getName());
            container.set(ACTIVE_KEY, PersistentDataType.BOOLEAN, data.isActive());
        }

        @Override
        public @NotNull TestBlockData deserialize(@NotNull PersistentDataContainer container) {
            int level = container.getOrDefault(LEVEL_KEY, PersistentDataType.INTEGER, 1);
            String name = container.getOrDefault(NAME_KEY, PersistentDataType.STRING, "default");
            boolean active = container.getOrDefault(ACTIVE_KEY, PersistentDataType.BOOLEAN, false);
            return new TestBlockData(level, name, active);
        }

        @Override
        public boolean hasData(@NotNull PersistentDataContainer container) {
            return container.has(LEVEL_KEY, PersistentDataType.INTEGER);
        }
    }

    // Test SmartBlock with data
    public static class TestSmartBlockWithData extends SmartBlock implements DataHolder<TestBlockData> {
        public TestSmartBlockWithData() {
            super("test_with_data", "Test Block With Data");
        }

        @Override
        public Class<TestBlockData> getDataType() {
            return TestBlockData.class;
        }

        @Override
        public TestBlockDataSerializer getDataSerializer() {
            return new TestBlockDataSerializer();
        }

        @Override
        public TestBlockData createDefaultData() {
            return new TestBlockData(1, "default", false);
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("SmartBlock with data should support data")
    void testBlockWithDataSupportsData() {
        TestSmartBlockWithData smartBlock = new TestSmartBlockWithData();

        assertNotNull(smartBlock.getDataType());
        assertEquals(TestBlockData.class, smartBlock.getDataType());
        assertNotNull(smartBlock.getDataSerializer());
    }

    @Test
    @DisplayName("SmartBlock should create default data correctly")
    void testDefaultDataCreation() {
        TestSmartBlockWithData smartBlock = new TestSmartBlockWithData();
        TestBlockData defaultData = smartBlock.createDefaultData();

        assertEquals(1, defaultData.getLevel());
        assertEquals("default", defaultData.getName());
        assertFalse(defaultData.isActive());
    }

    @Test
    @DisplayName("SmartBlock without data should throw exception on createDefaultData")
    void testBlockWithoutDataThrowsException() {
        TestSmartBlockWithData smartBlock = new TestSmartBlockWithData();

        assertThrows(UnsupportedOperationException.class, smartBlock::createDefaultData);
    }

    @Test
    @DisplayName("Serializer should serialize and deserialize data correctly")
    void testSerializerFullCycle() {
        TestBlockDataSerializer serializer = new TestBlockDataSerializer();
        TestBlockData originalData = new TestBlockData(42, "advanced", true);

        // Create a new PDC container for testing
        PersistentDataContainer container = createPersistentDataContainer();

        // Serialize the data
        serializer.serialize(originalData, container);

        // Verify that the container has the expected data
        assertTrue(container.has(new NamespacedKey("test", "level"), PersistentDataType.INTEGER));
        assertTrue(container.has(new NamespacedKey("test", "name"), PersistentDataType.STRING));
        assertTrue(container.has(new NamespacedKey("test", "active"), PersistentDataType.BOOLEAN));

        // Verify the values are correct
        assertEquals(42, container.get(new NamespacedKey("test", "level"), PersistentDataType.INTEGER));
        assertEquals("advanced", container.get(new NamespacedKey("test", "name"), PersistentDataType.STRING));
        assertEquals(true, container.get(new NamespacedKey("test", "active"), PersistentDataType.BOOLEAN));

        // Deserialize the data
        TestBlockData deserializedData = serializer.deserialize(container);

        // Verify that the deserialized data matches the original
        assertEquals(originalData.getLevel(), deserializedData.getLevel());
        assertEquals(originalData.getName(), deserializedData.getName());
        assertEquals(originalData.isActive(), deserializedData.isActive());
        assertEquals(originalData, deserializedData);
    }

    @Test
    @DisplayName("Serializer should handle empty container with defaults")
    void testSerializerWithEmptyContainer() {
        TestBlockDataSerializer serializer = new TestBlockDataSerializer();

        // Create an empty PDC container
        PersistentDataContainer emptyContainer = createPersistentDataContainer();

        // hasData should return false for empty container
        assertFalse(serializer.hasData(emptyContainer));

        // Deserialize should return default values
        TestBlockData defaultData = serializer.deserialize(emptyContainer);

        assertEquals(1, defaultData.getLevel());
        assertEquals("default", defaultData.getName());
        assertFalse(defaultData.isActive());
    }

    @Test
    @DisplayName("Serializer should handle partial data gracefully")
    void testSerializerWithPartialData() {
        TestBlockDataSerializer serializer = new TestBlockDataSerializer();

        // Create a PDC container with only some data
        PersistentDataContainer partialContainer = createPersistentDataContainer();
        partialContainer.set(new NamespacedKey("test", "level"), PersistentDataType.INTEGER, 10);
        partialContainer.set(new NamespacedKey("test", "name"), PersistentDataType.STRING, "partial");
        // Note: active boolean is missing

        // hasData should return true because level exists
        assertTrue(serializer.hasData(partialContainer));

        // Deserialize should use defaults for missing values
        TestBlockData partialData = serializer.deserialize(partialContainer);

        assertEquals(10, partialData.getLevel());
        assertEquals("partial", partialData.getName());
        assertFalse(partialData.isActive()); // Should be default value
    }

    @Test
    @DisplayName("Serializer should handle edge case values correctly")
    void testSerializerWithEdgeCaseValues() {
        TestBlockDataSerializer serializer = new TestBlockDataSerializer();

        // Test with edge case values
        TestBlockData edgeCaseData = new TestBlockData(0, "", true);

        // Create a new PDC container for testing
        PersistentDataContainer container = createPersistentDataContainer();

        // Serialize the edge case data
        serializer.serialize(edgeCaseData, container);

        // Verify serialization
        assertEquals(0, container.get(new NamespacedKey("test", "level"), PersistentDataType.INTEGER));
        assertEquals("", container.get(new NamespacedKey("test", "name"), PersistentDataType.STRING));
        assertEquals(true, container.get(new NamespacedKey("test", "active"), PersistentDataType.BOOLEAN));

        // Deserialize and verify
        TestBlockData deserializedEdgeCase = serializer.deserialize(container);

        assertEquals(0, deserializedEdgeCase.getLevel());
        assertEquals("", deserializedEdgeCase.getName());
        assertTrue(deserializedEdgeCase.isActive());
        assertEquals(edgeCaseData, deserializedEdgeCase);
    }

    private PersistentDataContainer createPersistentDataContainer() {
        return ItemStack.of(Material.STONE).getItemMeta().getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
    }

    @Test
    @DisplayName("Serializer should have correct metadata")
    void testSerializerMetadata() {
        TestBlockDataSerializer serializer = new TestBlockDataSerializer();

        assertEquals(TestBlockData.class, serializer.getType());
        assertNotNull(serializer.getKey());
        assertEquals("test", serializer.getKey().getNamespace());
        assertEquals("data", serializer.getKey().getKey());
    }
}