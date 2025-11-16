package me.mykindos.betterpvp.core.block;

import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SmartBlockDataIntegrationTest {

    // Test data class
    public static class FurnaceData {
        private final int fuel;
        private final int cookTime;
        private String lastPlayer;

        public FurnaceData(int fuel, int cookTime, String lastPlayer) {
            this.fuel = fuel;
            this.cookTime = cookTime;
            this.lastPlayer = lastPlayer;
        }

        // Getters and setters
        public int getFuel() { return fuel; }
        public int getCookTime() { return cookTime; }
        public String getLastPlayer() { return lastPlayer; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FurnaceData that = (FurnaceData) obj;
            return fuel == that.fuel && cookTime == that.cookTime && 
                   (Objects.equals(lastPlayer, that.lastPlayer));
        }
    }

    // Test serializer for FurnaceData
    public static class FurnaceDataSerializer implements SmartBlockDataSerializer<FurnaceData> {

        @Override
        public @NotNull Class<FurnaceData> getType() {
            return FurnaceData.class;
        }

        @Override
        public byte[] serializeToBytes(@NotNull FurnaceData data) throws IOException {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 DataOutputStream dos = new DataOutputStream(baos)) {
                
                // Write fuel
                dos.writeInt(data.getFuel());
                
                // Write cook time
                dos.writeInt(data.getCookTime());
                
                // Write last player (handle null as empty string)
                String lastPlayer = data.getLastPlayer() != null ? data.getLastPlayer() : "";
                dos.writeUTF(lastPlayer);
                
                dos.flush();
                return baos.toByteArray();
            }
        }

        @Override
        public @NotNull FurnaceData deserializeFromBytes(byte[] bytes) throws IOException {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 DataInputStream dis = new DataInputStream(bais)) {
                
                // Read fuel
                int fuel = dis.readInt();
                
                // Read cook time
                int cookTime = dis.readInt();
                
                // Read last player
                String lastPlayer = dis.readUTF();
                
                return new FurnaceData(fuel, cookTime, lastPlayer);
            }
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Furnace data serializer should serialize and deserialize correctly")
    void testFurnaceDataSerialization() throws IOException {
        FurnaceDataSerializer serializer = new FurnaceDataSerializer();
        
        // Test data
        FurnaceData originalData = new FurnaceData(100, 50, "TestPlayer");
        
        // Serialize to bytes
        byte[] serializedBytes = serializer.serializeToBytes(originalData);
        
        // Deserialize from bytes
        FurnaceData deserializedData = serializer.deserializeFromBytes(serializedBytes);
        
        // Verify deserialization
        assertEquals(originalData, deserializedData);
        assertEquals(100, deserializedData.getFuel());
        assertEquals(50, deserializedData.getCookTime());
        assertEquals("TestPlayer", deserializedData.getLastPlayer());
    }

    @Test
    @DisplayName("Furnace data serializer should handle edge cases")
    void testFurnaceDataSerializationEdgeCases() throws IOException {
        FurnaceDataSerializer serializer = new FurnaceDataSerializer();
        
        // Test with empty/default values
        FurnaceData emptyData = new FurnaceData(0, 0, "");
        byte[] emptyBytes = serializer.serializeToBytes(emptyData);
        FurnaceData deserializedEmpty = serializer.deserializeFromBytes(emptyBytes);
        assertEquals(emptyData, deserializedEmpty);
        
        // Test with null last player (should be converted to empty string)
        FurnaceData nullPlayerData = new FurnaceData(50, 25, null);
        byte[] nullPlayerBytes = serializer.serializeToBytes(nullPlayerData);
        FurnaceData deserializedNullPlayer = serializer.deserializeFromBytes(nullPlayerBytes);
        assertEquals("", deserializedNullPlayer.getLastPlayer());
        
        // Test with special characters in player name
        FurnaceData specialData = new FurnaceData(200, 100, "Player_with_special_chars!@#$%");
        byte[] specialBytes = serializer.serializeToBytes(specialData);
        FurnaceData deserializedSpecial = serializer.deserializeFromBytes(specialBytes);
        assertEquals(specialData, deserializedSpecial);
    }
} 