package me.mykindos.betterpvp.core.block;

import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmartBlockDataIntegrationTest {

    private ServerMock server;
    private World world;
    private SmartBlockDataManager dataManager;

    // Test data class
    public static class FurnaceData {
        private int fuel;
        private int cookTime;
        private String lastPlayer = "";

        public FurnaceData(int fuel, int cookTime, String lastPlayer) {
            this.fuel = fuel;
            this.cookTime = cookTime;
            this.lastPlayer = lastPlayer;
        }

        // Getters and setters
        public int getFuel() { return fuel; }
        public void setFuel(int fuel) { this.fuel = fuel; }
        public int getCookTime() { return cookTime; }
        public void setCookTime(int cookTime) { this.cookTime = cookTime; }
        public String getLastPlayer() { return lastPlayer; }
        public void setLastPlayer(String lastPlayer) { this.lastPlayer = lastPlayer; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FurnaceData that = (FurnaceData) obj;
            return fuel == that.fuel && cookTime == that.cookTime && 
                   (lastPlayer != null ? lastPlayer.equals(that.lastPlayer) : that.lastPlayer == null);
        }
    }

    // Test serializer for FurnaceData
    public static class FurnaceDataSerializer implements SmartBlockDataSerializer<FurnaceData> {
        private static final NamespacedKey FUEL_KEY = new NamespacedKey("test", "fuel");
        private static final NamespacedKey COOK_TIME_KEY = new NamespacedKey("test", "cook_time");
        private static final NamespacedKey LAST_PLAYER_KEY = new NamespacedKey("test", "last_player");

        @Override
        public @NotNull NamespacedKey getKey() {
            return new NamespacedKey("test", "furnace_data");
        }

        @Override
        public @NotNull Class<FurnaceData> getType() {
            return FurnaceData.class;
        }

        @Override
        public void serialize(@NotNull FurnaceData data, @NotNull PersistentDataContainer container) {
            container.set(FUEL_KEY, PersistentDataType.INTEGER, data.getFuel());
            container.set(COOK_TIME_KEY, PersistentDataType.INTEGER, data.getCookTime());
            container.set(LAST_PLAYER_KEY, PersistentDataType.STRING, data.getLastPlayer());
        }

        @Override
        public @NotNull FurnaceData deserialize(@NotNull PersistentDataContainer container) {
            int fuel = container.getOrDefault(FUEL_KEY, PersistentDataType.INTEGER, 0);
            int cookTime = container.getOrDefault(COOK_TIME_KEY, PersistentDataType.INTEGER, 0);
            String lastPlayer = container.getOrDefault(LAST_PLAYER_KEY, PersistentDataType.STRING, "");
            return new FurnaceData(fuel, cookTime, lastPlayer);
        }

        @Override
        public boolean hasData(@NotNull PersistentDataContainer container) {
            return container.has(FUEL_KEY, PersistentDataType.INTEGER);
        }
    }

    // Test SmartBlock with data
    public static class SmartFurnace extends SmartBlock implements DataHolder<FurnaceData> {
        public SmartFurnace() {
            super("smart_furnace", "Smart Furnace");
        }

        @Override
        public Class<FurnaceData> getDataType() {
            return FurnaceData.class;
        }

        @Override
        public FurnaceDataSerializer getDataSerializer() {
            return new FurnaceDataSerializer();
        }

        @Override
        public FurnaceData createDefaultData() {
            return new FurnaceData(0, 0, "");
        }
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        dataManager = new SmartBlockDataManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Block data should be created with default values on first placement")
    void testDefaultDataCreationOnPlacement() {
        Block block = world.getBlockAt(10, 64, 10);
        block.setType(Material.FURNACE);
        
        SmartBlock smartBlock = new SmartFurnace();
        SmartBlockInstance instance = new SmartBlockInstance(smartBlock, block, dataManager);
        
        assertTrue(instance.supportsData());
        
        SmartBlockData<FurnaceData> blockData = instance.getBlockData();
        assertNotNull(blockData);
        FurnaceData data = blockData.get();
        
        assertEquals(0, data.getFuel());
        assertEquals(0, data.getCookTime());
        assertEquals("", data.getLastPlayer());
        
        // Verify PDC contains the serialized default data
        verifyPDCContainsData(block, new NamespacedKey("test", "furnace_data"), 0, 0, "");
    }

    @Test
    @DisplayName("Block data should persist modifications to PDC")
    void testDataPersistenceToPDC() {
        Block block = world.getBlockAt(15, 64, 15);
        block.setType(Material.FURNACE);
        
        SmartBlock smartBlock = new SmartFurnace();
        SmartBlockInstance instance = new SmartBlockInstance(smartBlock, block, dataManager);
        
        // Get and modify data
        SmartBlockData<FurnaceData> blockData = instance.getBlockData();
        assertNotNull(blockData);
        blockData.update(data -> {
            data.setFuel(100);
            data.setCookTime(50);
            data.setLastPlayer("TestPlayer");
        });
        
        // Verify data is updated in memory
        FurnaceData updatedData = blockData.get();
        assertEquals(100, updatedData.getFuel());
        assertEquals(50, updatedData.getCookTime());
        assertEquals("TestPlayer", updatedData.getLastPlayer());
        
        // Verify PDC contains the updated data
        verifyPDCContainsData(block, new NamespacedKey("test", "furnace_data"), 100, 50, "TestPlayer");
    }

    @Test
    @DisplayName("Block data should load correctly from existing PDC data")
    void testDataLoadingFromExistingPDC() {
        Block block = world.getBlockAt(20, 64, 20);
        block.setType(Material.FURNACE);
        
        // Pre-populate PDC with furnace data
        populatePDCWithData(block, new NamespacedKey("test", "furnace_data"), 75, 25, "ExistingPlayer");
        
        SmartBlock smartBlock = new SmartFurnace();
        SmartBlockInstance instance = new SmartBlockInstance(smartBlock, block, dataManager);
        
        // Get data - should load from existing PDC
        FurnaceData data = instance.getData();
        assertNotNull(data);

        assertEquals(75, data.getFuel());
        assertEquals(25, data.getCookTime());
        assertEquals("ExistingPlayer", data.getLastPlayer());
        
        // Verify PDC still contains the original data
        verifyPDCContainsData(block, new NamespacedKey("test", "furnace_data"), 75, 25, "ExistingPlayer");
    }

    @Test
    @DisplayName("Different block instances should have independent data and PDC storage")
    void testIndependentBlockData() {
        Block block1 = world.getBlockAt(25, 64, 25);
        Block block2 = world.getBlockAt(30, 64, 30);
        block1.setType(Material.FURNACE);
        block2.setType(Material.FURNACE);
        
        SmartBlock smartBlock1 = new SmartFurnace();
        SmartBlock smartBlock2 = new SmartFurnace();
        SmartBlockInstance instance1 = new SmartBlockInstance(smartBlock1, block1, dataManager);
        SmartBlockInstance instance2 = new SmartBlockInstance(smartBlock2, block2, dataManager);
        
        // Modify data for first block
        SmartBlockData<FurnaceData> blockData1 = instance1.getBlockData();
        assertNotNull(blockData1);
        blockData1.update(data -> {
            data.setFuel(200);
            data.setLastPlayer("Player1");
        });
        
        // Modify data for second block
        SmartBlockData<FurnaceData> blockData2 = instance2.getBlockData();
        assertNotNull(blockData2);
        blockData2.update(data -> {
            data.setFuel(300);
            data.setLastPlayer("Player2");
        });
        
        // Verify blocks have independent data
        assertEquals(200, blockData1.get().getFuel());
        assertEquals("Player1", blockData1.get().getLastPlayer());
        
        assertEquals(300, blockData2.get().getFuel());
        assertEquals("Player2", blockData2.get().getLastPlayer());
        
        // Verify PDC stores independent data for each block
        verifyPDCContainsData(block1, new NamespacedKey("test", "furnace_data"), 200, 0, "Player1");
        verifyPDCContainsData(block2, new NamespacedKey("test", "furnace_data"), 300, 0, "Player2");
    }

    @Test
    @DisplayName("Block data should return same instance from cache on multiple accesses")
    void testDataCaching() {
        Block block = world.getBlockAt(35, 64, 35);
        block.setType(Material.FURNACE);
        
        SmartBlock smartBlock = new SmartFurnace();
        SmartBlockInstance instance = new SmartBlockInstance(smartBlock, block, dataManager);
        
        // Get data multiple times
        SmartBlockData<FurnaceData> blockData1 = instance.getBlockData();
        SmartBlockData<FurnaceData> blockData2 = instance.getBlockData();
        SmartBlockData<FurnaceData> blockData3 = instance.getBlockData();
        
        assertNotNull(blockData1);
        assertNotNull(blockData2);
        assertNotNull(blockData3);
        
        // Should return the same cached instance
        assertSame(blockData1.get(), blockData2.get());
        assertSame(blockData2.get(), blockData3.get());
    }

    @Test
    @DisplayName("Block data should persist complex modifications to PDC")
    void testComplexDataPersistenceToPDC() {
        Block block = world.getBlockAt(40, 64, 40);
        block.setType(Material.FURNACE);
        
        SmartBlock smartBlock = new SmartFurnace();
        SmartBlockInstance instance = new SmartBlockInstance(smartBlock, block, dataManager);
        
        // Get data and perform multiple modifications
        SmartBlockData<FurnaceData> blockData = instance.getBlockData();
        assertNotNull(blockData);
        
        // First modification
        blockData.update(data -> {
            data.setFuel(50);
            data.setCookTime(10);
        });
        verifyPDCContainsData(block, new NamespacedKey("test", "furnace_data"), 50, 10, "");
        
        // Second modification
        blockData.update(data -> {
            data.setFuel(data.getFuel() + 25); // 75
            data.setLastPlayer("ComplexPlayer");
        });
        verifyPDCContainsData(block, new NamespacedKey("test", "furnace_data"), 75, 10, "ComplexPlayer");
        
        // Third modification
        blockData.update(data -> {
            data.setCookTime(data.getCookTime() * 2); // 20
            data.setFuel(0); // Reset fuel
        });
        verifyPDCContainsData(block, new NamespacedKey("test", "furnace_data"), 0, 20, "ComplexPlayer");
    }

    @Test
    @DisplayName("Block data should correctly handle empty and null string values in PDC")
    void testPDCHandlingOfEdgeCases() {
        Block block = world.getBlockAt(45, 64, 45);
        block.setType(Material.FURNACE);
        
        SmartBlock smartBlock = new SmartFurnace();
        SmartBlockInstance instance = new SmartBlockInstance(smartBlock, block, dataManager);
        
        // Test with empty string
        SmartBlockData<FurnaceData> blockData = instance.getBlockData();
        assertNotNull(blockData);
        blockData.update(data -> {
            data.setFuel(99);
            data.setLastPlayer(""); // Empty string
        });
        
        verifyPDCContainsData(block, new NamespacedKey("test", "furnace_data"), 99, 0, "");
        
        // Verify data can be loaded back correctly
        SmartBlockInstance newInstance = new SmartBlockInstance(smartBlock, block, new SmartBlockDataManager());
        FurnaceData reloadedData = newInstance.getData();
        assertNotNull(reloadedData);
        assertEquals(99, reloadedData.getFuel());
        assertEquals("", reloadedData.getLastPlayer());
    }

    /**
     * Helper method to verify that PDC contains the expected furnace data
     */
    private void verifyPDCContainsData(Block block, NamespacedKey dataKey, int expectedFuel, int expectedCookTime, String expectedPlayer) {
        PersistentDataContainer blockPDC = UtilBlock.getPersistentDataContainer(block);
        
        // Verify the data container exists for our serializer
        assertTrue(blockPDC.has(dataKey, PersistentDataType.TAG_CONTAINER), 
                   "Block PDC should contain data container for key: " + dataKey);
        
        PersistentDataContainer dataContainer = blockPDC.get(dataKey, PersistentDataType.TAG_CONTAINER);
        assertNotNull(dataContainer, "Data container should not be null");
        
        // Verify individual data fields
        assertTrue(dataContainer.has(new NamespacedKey("test", "fuel"), PersistentDataType.INTEGER),
                   "Data container should contain fuel value");
        assertTrue(dataContainer.has(new NamespacedKey("test", "cook_time"), PersistentDataType.INTEGER),
                   "Data container should contain cook_time value");
        assertTrue(dataContainer.has(new NamespacedKey("test", "last_player"), PersistentDataType.STRING),
                   "Data container should contain last_player value");
        
        assertEquals(expectedFuel, dataContainer.get(new NamespacedKey("test", "fuel"), PersistentDataType.INTEGER));
        assertEquals(expectedCookTime, dataContainer.get(new NamespacedKey("test", "cook_time"), PersistentDataType.INTEGER));
        assertEquals(expectedPlayer, dataContainer.get(new NamespacedKey("test", "last_player"), PersistentDataType.STRING));
    }

    /**
     * Helper method to populate PDC with furnace data for testing data loading
     */
    private void populatePDCWithData(Block block, NamespacedKey dataKey, int fuel, int cookTime, String lastPlayer) {
        PersistentDataContainer blockPDC = UtilBlock.getPersistentDataContainer(block);
        
        // Create data container
        PersistentDataContainer dataContainer = blockPDC.getAdapterContext().newPersistentDataContainer();
        dataContainer.set(new NamespacedKey("test", "fuel"), PersistentDataType.INTEGER, fuel);
        dataContainer.set(new NamespacedKey("test", "cook_time"), PersistentDataType.INTEGER, cookTime);
        dataContainer.set(new NamespacedKey("test", "last_player"), PersistentDataType.STRING, lastPlayer);
        
        // Store in block PDC
        blockPDC.set(dataKey, PersistentDataType.TAG_CONTAINER, dataContainer);
        UtilBlock.setPersistentDataContainer(block, blockPDC);
    }
} 