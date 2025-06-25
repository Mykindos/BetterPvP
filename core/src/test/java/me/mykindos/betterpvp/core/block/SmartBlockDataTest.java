package me.mykindos.betterpvp.core.block;

import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for SmartBlockData serialization functionality.
 * Tests serializer behavior with fake PDC implementation.
 */
public class SmartBlockDataTest {

    // Test data classes
    public static class SimpleData {
        private final String name;
        private final int value;
        
        public SimpleData(String name, int value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() { return name; }
        public int getValue() { return value; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SimpleData that = (SimpleData) obj;
            return value == that.value && (Objects.equals(name, that.name));
        }
    }
    
    public static class ComplexData {
        private final List<String> items;
        private final boolean enabled;
        private final double multiplier;
        private final String owner;
        
        public ComplexData(List<String> items, boolean enabled, double multiplier, String owner) {
            this.items = new ArrayList<>(items);
            this.enabled = enabled;
            this.multiplier = multiplier;
            this.owner = owner;
        }
        
        public List<String> getItems() { return items; }
        public boolean isEnabled() { return enabled; }
        public double getMultiplier() { return multiplier; }
        public String getOwner() { return owner; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ComplexData that = (ComplexData) obj;
            return enabled == that.enabled && 
                   Double.compare(that.multiplier, multiplier) == 0 &&
                   (Objects.equals(items, that.items)) &&
                   (Objects.equals(owner, that.owner));
        }
    }

    // Test serializers
    public static class SimpleDataSerializer implements SmartBlockDataSerializer<SimpleData> {

        @NotNull
        private NamespacedKey getKey() {
            return new NamespacedKey("test", "simple_data");
        }

        @Override
        public @NotNull Class<SimpleData> getType() {
            return SimpleData.class;
        }

        @Override
        public byte[] serializeToBytes(@NotNull SimpleData data) throws IOException {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 DataOutputStream dos = new DataOutputStream(baos)) {
                
                // Write name (handle null as empty string)
                String name = data.getName() != null ? data.getName() : "";
                dos.writeUTF(name);
                
                // Write value
                dos.writeInt(data.getValue());
                
                dos.flush();
                return baos.toByteArray();
            }
        }

        @Override
        public @NotNull SimpleData deserializeFromBytes(byte[] bytes) throws IOException {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 DataInputStream dis = new DataInputStream(bais)) {
                
                // Read name
                String name = dis.readUTF();
                
                // Read value
                int value = dis.readInt();
                
                return new SimpleData(name, value);
            }
        }
    }
    
    public static class ComplexDataSerializer implements SmartBlockDataSerializer<ComplexData> {

        @NotNull
        private NamespacedKey getKey() {
            return new NamespacedKey("test", "complex_data");
        }

        @Override
        public @NotNull Class<ComplexData> getType() {
            return ComplexData.class;
        }

        @Override
        public byte[] serializeToBytes(@NotNull ComplexData data) throws IOException {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 DataOutputStream dos = new DataOutputStream(baos)) {
                
                // Write list of items
                List<String> items = data.getItems() != null ? data.getItems() : new ArrayList<>();
                dos.writeInt(items.size());
                for (String item : items) {
                    dos.writeUTF(item != null ? item : "");
                }
                
                // Write enabled flag
                dos.writeBoolean(data.isEnabled());
                
                // Write multiplier
                dos.writeDouble(data.getMultiplier());
                
                // Write owner (handle null as empty string)
                String owner = data.getOwner() != null ? data.getOwner() : "";
                dos.writeUTF(owner);
                
                dos.flush();
                return baos.toByteArray();
            }
        }

        @Override
        public @NotNull ComplexData deserializeFromBytes(byte[] bytes) throws IOException {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 DataInputStream dis = new DataInputStream(bais)) {
                
                // Read list of items
                int itemCount = dis.readInt();
                List<String> items = new ArrayList<>();
                for (int i = 0; i < itemCount; i++) {
                    items.add(dis.readUTF());
                }
                
                // Read enabled flag
                boolean enabled = dis.readBoolean();
                
                // Read multiplier
                double multiplier = dis.readDouble();
                
                // Read owner
                String owner = dis.readUTF();
                
                return new ComplexData(items, enabled, multiplier, owner);
            }
        }
    }

    @Test
    @DisplayName("Simple data serializer should serialize and deserialize correctly")
    void testSimpleDataSerialization() throws IOException {
        SimpleDataSerializer serializer = new SimpleDataSerializer();
        
        // Test data
        SimpleData originalData = new SimpleData("TestName", 42);
        
        // Serialize
        byte[] serializedBytes = serializer.serializeToBytes(originalData);
        
        // Verify serialization
        assertEquals(originalData, serializer.deserializeFromBytes(serializedBytes));
    }

    @Test
    @DisplayName("Complex data serializer should handle lists and multiple data types")
    void testComplexDataSerialization() throws IOException {
        ComplexDataSerializer serializer = new ComplexDataSerializer();

        // Test data with complex structure
        List<String> items = List.of("item1", "item2", "item3");
        ComplexData originalData = new ComplexData(items, true, 2.5, "TestOwner");
        
        // Serialize
        byte[] serializedBytes = serializer.serializeToBytes(originalData);
        
        // Verify serialization
        ComplexData deserializedData = serializer.deserializeFromBytes(serializedBytes);
        
        // Verify deserialization
        assertEquals(originalData, deserializedData);
        assertEquals(items, deserializedData.getItems());
        assertTrue(deserializedData.isEnabled());
        assertEquals(2.5, deserializedData.getMultiplier());
        assertEquals("TestOwner", deserializedData.getOwner());
    }

    @Test
    @DisplayName("Serializers should handle empty and default values")
    void testSerializationWithEmptyValues() throws IOException {
        SimpleDataSerializer simpleSerializer = new SimpleDataSerializer();
        ComplexDataSerializer complexSerializer = new ComplexDataSerializer();
        
        // Test simple data with empty values
        SimpleData emptySimpleData = new SimpleData("", 0);
        
        byte[] serializedBytes = simpleSerializer.serializeToBytes(emptySimpleData);
        SimpleData deserializedSimple = simpleSerializer.deserializeFromBytes(serializedBytes);
        
        assertEquals(emptySimpleData, deserializedSimple);
        assertEquals("", deserializedSimple.getName());
        assertEquals(0, deserializedSimple.getValue());
        
        // Test complex data with empty values
        ComplexData emptyComplexData = new ComplexData(new ArrayList<>(), false, 0.0, "");
        
        byte[] serializedComplexBytes = complexSerializer.serializeToBytes(emptyComplexData);
        ComplexData deserializedComplex = complexSerializer.deserializeFromBytes(serializedComplexBytes);
        
        assertEquals(emptyComplexData, deserializedComplex);
        assertTrue(deserializedComplex.getItems().isEmpty());
        assertFalse(deserializedComplex.isEnabled());
        assertEquals(0.0, deserializedComplex.getMultiplier());
        assertEquals("", deserializedComplex.getOwner());
    }

    @Test
    @DisplayName("Serializers should handle partial data containers")
    void testPartialDataDeserialization() throws IOException {
        SimpleDataSerializer serializer = new SimpleDataSerializer();

        byte[] serializedBytes = serializer.serializeToBytes(new SimpleData("PartialName", 0));
        SimpleData deserializedData = serializer.deserializeFromBytes(serializedBytes);
        assertEquals("PartialName", deserializedData.getName());
        assertEquals(0, deserializedData.getValue()); // Should use default value
    }

    @Test
    @DisplayName("Serializers should correctly detect empty containers")
    void testEmptyContainerDetection() throws IOException {
        SimpleDataSerializer simpleSerializer = new SimpleDataSerializer();
        ComplexDataSerializer complexSerializer = new ComplexDataSerializer();
        
        byte[] serializedBytes = simpleSerializer.serializeToBytes(new SimpleData("", 0));
        SimpleData defaultSimple = simpleSerializer.deserializeFromBytes(serializedBytes);
        assertEquals("", defaultSimple.getName());
        assertEquals(0, defaultSimple.getValue());
        
        byte[] serializedComplexBytes = complexSerializer.serializeToBytes(new ComplexData(new ArrayList<>(), false, 1.0, ""));
        ComplexData defaultComplex = complexSerializer.deserializeFromBytes(serializedComplexBytes);
        assertTrue(defaultComplex.getItems().isEmpty());
        assertFalse(defaultComplex.isEnabled());
        assertEquals(1.0, defaultComplex.getMultiplier()); // Default multiplier
        assertEquals("", defaultComplex.getOwner());
    }

    @Test
    @DisplayName("Serializers should handle special characters and edge cases")
    void testSerializationEdgeCases() throws IOException {
        SimpleDataSerializer serializer = new SimpleDataSerializer();
        
        // Test with special characters
        SimpleData specialData = new SimpleData("Test,With,Commas!@#$%^&*()", Integer.MAX_VALUE);
        
        byte[] serializedBytes = serializer.serializeToBytes(specialData);
        SimpleData deserializedData = serializer.deserializeFromBytes(serializedBytes);
        
        assertEquals(specialData, deserializedData);
        assertEquals("Test,With,Commas!@#$%^&*()", deserializedData.getName());
        assertEquals(Integer.MAX_VALUE, deserializedData.getValue());
        
        // Test complex data with edge cases
        ComplexDataSerializer complexSerializer = new ComplexDataSerializer();
        
        List<String> edgeCaseItems = List.of("", "item,with,commas", "item with spaces", "special!@#");
        ComplexData edgeCaseData = new ComplexData(edgeCaseItems, true, Double.MAX_VALUE, "owner,with,commas");
        
        byte[] serializedComplexBytes = complexSerializer.serializeToBytes(edgeCaseData);
        ComplexData deserializedComplexData = complexSerializer.deserializeFromBytes(serializedComplexBytes);
        
        assertEquals(edgeCaseData, deserializedComplexData);
        assertEquals(edgeCaseItems, deserializedComplexData.getItems());
        assertEquals(Double.MAX_VALUE, deserializedComplexData.getMultiplier());
        assertEquals("owner,with,commas", deserializedComplexData.getOwner());
    }

    @Test
    @DisplayName("Serializer keys and types should be consistent")
    void testSerializerMetadata() {
        SimpleDataSerializer simpleSerializer = new SimpleDataSerializer();
        ComplexDataSerializer complexSerializer = new ComplexDataSerializer();
        
        // Test keys are properly formatted
        assertEquals("test", simpleSerializer.getKey().getNamespace());
        assertEquals("simple_data", simpleSerializer.getKey().getKey());
        assertEquals("test", complexSerializer.getKey().getNamespace());
        assertEquals("complex_data", complexSerializer.getKey().getKey());
        
        // Test types
        assertEquals(SimpleData.class, simpleSerializer.getType());
        assertEquals(ComplexData.class, complexSerializer.getType());
        
        // Test keys are unique
        assertNotEquals(simpleSerializer.getKey(), complexSerializer.getKey());
    }
} 