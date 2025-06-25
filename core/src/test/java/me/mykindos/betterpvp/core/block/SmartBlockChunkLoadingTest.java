package me.mykindos.betterpvp.core.block;

import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.UnloadHandler;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmartBlockChunkLoadingTest {

    private ServerMock server;

    // Test data class that implements UnloadHandler
    public static class SmelterData implements UnloadHandler {
        private final int fuel;
        private final int processTime;
        private String owner;
        private final AtomicBoolean unloadCalled = new AtomicBoolean(false);
        private final AtomicInteger unloadCount = new AtomicInteger(0);

        public SmelterData(int fuel, int processTime, String owner) {
            this.fuel = fuel;
            this.processTime = processTime;
            this.owner = owner;
        }

        // Getters and setters
        public int getFuel() { return fuel; }
        public int getProcessTime() { return processTime; }
        public String getOwner() { return owner; }

        // UnloadHandler implementation for testing
        @Override
        public void onUnload(@NotNull SmartBlockInstance instance) {
            unloadCalled.set(true);
            unloadCount.incrementAndGet();
        }

        // Test verification methods
        public boolean wasUnloadCalled() { return unloadCalled.get(); }
        public int getUnloadCount() { return unloadCount.get(); }
        public void resetUnloadState() { 
            unloadCalled.set(false); 
            unloadCount.set(0);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SmelterData that = (SmelterData) obj;
            return fuel == that.fuel && processTime == that.processTime && 
                   (Objects.equals(owner, that.owner));
        }
    }

    // Test serializer for SmelterData
    public static class SmelterDataSerializer implements SmartBlockDataSerializer<SmelterData> {

        @Override
        public @NotNull NamespacedKey getKey() {
            return new NamespacedKey("test", "smelter_data");
        }

        @Override
        public @NotNull Class<SmelterData> getType() {
            return SmelterData.class;
        }

        @Override
        public byte[] serializeToBytes(@NotNull SmelterData data) throws IOException {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 DataOutputStream dos = new DataOutputStream(baos)) {
                
                dos.writeInt(data.getFuel());
                dos.writeInt(data.getProcessTime());
                dos.writeUTF(data.getOwner() != null ? data.getOwner() : "");
                
                dos.flush();
                return baos.toByteArray();
            }
        }

        @Override
        public @NotNull SmelterData deserializeFromBytes(byte[] bytes) throws IOException {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 DataInputStream dis = new DataInputStream(bais)) {
                
                int fuel = dis.readInt();
                int processTime = dis.readInt();
                String owner = dis.readUTF();
                
                return new SmelterData(fuel, processTime, owner);
            }
        }
    }

    // Test SmartBlock with data
    public static class SmartSmelter extends SmartBlock implements DataHolder<SmelterData> {
        public SmartSmelter() {
            super("smart_smelter", "Smart Smelter");
        }

        @Override
        public Class<SmelterData> getDataType() {
            return SmelterData.class;
        }

        @Override
        public SmartBlockDataSerializer<SmelterData> getDataSerializer() {
            return new SmelterDataSerializer();
        }

        @Override
        public SmelterData createDefaultData() {
            return new SmelterData(0, 0, "");
        }
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("UnloadHandler should be called when onUnload is invoked")
    void testUnloadHandlerFunctionality() {
        // Arrange
        SmelterData smelterData = new SmelterData(100, 50, "TestPlayer");
        SmartSmelter smartSmelter = new SmartSmelter();
        
        // Create a mock block and instance for testing
        var world = server.addSimpleWorld("world");
        var block = world.getBlockAt(0, 64, 0);
        SmartBlockInstance instance = new SmartBlockInstance(smartSmelter, block, null);

        // Act
        smelterData.onUnload(instance);

        // Assert
        assertTrue(smelterData.wasUnloadCalled(), "UnloadHandler.onUnload should have been called");
        assertEquals(1, smelterData.getUnloadCount(), "onUnload should have been called exactly once");
    }

    @Test
    @DisplayName("UnloadHandler can be called multiple times and tracks count correctly")
    void testUnloadHandlerMultipleCalls() {
        // Arrange
        SmelterData smelterData = new SmelterData(100, 50, "TestPlayer");
        SmartSmelter smartSmelter = new SmartSmelter();
        
        var world = server.addSimpleWorld("world");
        var block = world.getBlockAt(0, 64, 0);
        SmartBlockInstance instance = new SmartBlockInstance(smartSmelter, block, null);

        // Act
        smelterData.onUnload(instance);
        smelterData.onUnload(instance);
        smelterData.onUnload(instance);

        // Assert
        assertTrue(smelterData.wasUnloadCalled(), "UnloadHandler.onUnload should have been called");
        assertEquals(3, smelterData.getUnloadCount(), "onUnload should have been called three times");
    }

    @Test
    @DisplayName("UnloadHandler state can be reset correctly")
    void testUnloadHandlerReset() {
        // Arrange
        SmelterData smelterData = new SmelterData(100, 50, "TestPlayer");
        SmartSmelter smartSmelter = new SmartSmelter();
        
        var world = server.addSimpleWorld("world");
        var block = world.getBlockAt(0, 64, 0);
        SmartBlockInstance instance = new SmartBlockInstance(smartSmelter, block, null);

        // Act
        smelterData.onUnload(instance);
        assertTrue(smelterData.wasUnloadCalled());
        assertEquals(1, smelterData.getUnloadCount());
        
        smelterData.resetUnloadState();

        // Assert
        assertFalse(smelterData.wasUnloadCalled(), "Unload state should be reset");
        assertEquals(0, smelterData.getUnloadCount(), "Unload count should be reset");
    }

    @Test
    @DisplayName("Smelter data serialization should work correctly with UnloadHandler")
    void testSmelterDataSerialization() throws IOException {
        SmelterDataSerializer serializer = new SmelterDataSerializer();
        
        // Test data
        SmelterData originalData = new SmelterData(150, 75, "SerializationTest");
        
        // Serialize to bytes
        byte[] serializedBytes = serializer.serializeToBytes(originalData);
        
        // Deserialize from bytes
        SmelterData deserializedData = serializer.deserializeFromBytes(serializedBytes);
        
        // Verify deserialization
        assertEquals(originalData, deserializedData);
        assertEquals(150, deserializedData.getFuel());
        assertEquals(75, deserializedData.getProcessTime());
        assertEquals("SerializationTest", deserializedData.getOwner());
        
        // Verify UnloadHandler functionality is preserved
        assertFalse(deserializedData.wasUnloadCalled());
        assertEquals(0, deserializedData.getUnloadCount());
    }

    @Test
    @DisplayName("Smelter data should handle edge cases in serialization")
    void testSmelterDataSerializationEdgeCases() throws IOException {
        SmelterDataSerializer serializer = new SmelterDataSerializer();

        // Test with empty/default values
        SmelterData emptyData = new SmelterData(0, 0, "");
        byte[] emptyBytes = serializer.serializeToBytes(emptyData);
        SmelterData deserializedEmpty = serializer.deserializeFromBytes(emptyBytes);
        assertEquals(emptyData, deserializedEmpty);
        assertFalse(deserializedEmpty.wasUnloadCalled());

        // Test with null owner (should be converted to empty string)
        SmelterData nullOwnerData = new SmelterData(50, 25, null);
        byte[] nullOwnerBytes = serializer.serializeToBytes(nullOwnerData);
        SmelterData deserializedNullOwner = serializer.deserializeFromBytes(nullOwnerBytes);
        assertEquals("", deserializedNullOwner.getOwner());
        assertFalse(deserializedNullOwner.wasUnloadCalled());

        // Test with special characters in owner name
        SmelterData specialData = new SmelterData(200, 100, "Player_with_special_chars!@#$%");
        byte[] specialBytes = serializer.serializeToBytes(specialData);
        SmelterData deserializedSpecial = serializer.deserializeFromBytes(specialBytes);
        assertEquals(specialData, deserializedSpecial);
        assertFalse(deserializedSpecial.wasUnloadCalled());
    }

    @Test
    @DisplayName("SmartSmelter should implement DataHolder correctly")
    void testSmartSmelterDataHolder() {
        SmartSmelter smartSmelter = new SmartSmelter();

        // Test DataHolder implementation
        assertEquals(SmelterData.class, smartSmelter.getDataType());
        assertNotNull(smartSmelter.getDataSerializer());
        assertEquals("test", smartSmelter.getDataSerializer().getKey().getNamespace());
        assertEquals("smelter_data", smartSmelter.getDataSerializer().getKey().getKey());

        // Test default data creation
        SmelterData defaultData = smartSmelter.createDefaultData();
        assertNotNull(defaultData);
        assertEquals(0, defaultData.getFuel());
        assertEquals(0, defaultData.getProcessTime());
        assertEquals("", defaultData.getOwner());
        assertFalse(defaultData.wasUnloadCalled());
    }
} 