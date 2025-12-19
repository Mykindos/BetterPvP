package me.mykindos.betterpvp.core.utilities.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class WeighedListTest {

    private WeighedList<String> weighedList;

    @BeforeEach
    void setUp() {
        weighedList = new WeighedList<>();
    }

    @Test
    @DisplayName("Random selection from an empty list")
    void testRandomSelectionFromEmptyList() {
        assertNull(weighedList.random());
    }

    @Test
    @DisplayName("Random selection from a single category and element")
    void testRandomSelectionFromSingleCategoryAndElement() {
        weighedList.add(1, 1, "A");
        assertEquals("A", weighedList.random());
    }

    @Test
    @DisplayName("Random selection from multiple categories and elements")
    void testRandomSelectionFromMultipleCategoriesAndElements() {
        // Add categories and elements with weights
        weighedList.add(10, 1, "A");
        weighedList.add(5, 1, "B");
        weighedList.add(1, 1, "C");
        weighedList.add(50, 90, "D");
        weighedList.add(50, 10, "E");

        // Test random selections
        double numIterations = 10000;
        int countA = 0, countB = 0, countC = 0, countD = 0, countE = 0;

        for (int i = 0; i < numIterations; i++) {
            String randomElement = weighedList.random();
            switch (randomElement) {
                case "A" -> countA++;
                case "B" -> countB++;
                case "C" -> countC++;
                case "D" -> countD++;
                case "E" -> countE++;
                default -> fail("Unexpected element: " + randomElement);
            }
        }

        // Check if the observed frequencies match the expected probabilities
        final double totalWeight = weighedList.getTotalCategoryWeights();
        System.out.println("Total weight: " + totalWeight);
        System.out.println("A: " + countA + ", B: " + countB + ", C: " + countC + ", D: " + countD + ", E: " + countE);

        // Checking presence
        System.out.println("Checking presence...");
        final Collection<String> elements = weighedList.getElements();
        assertTrue(elements.contains("A"));
        assertTrue(elements.contains("B"));
        assertTrue(elements.contains("C"));
        assertTrue(elements.contains("D"));

        // Check if the observed frequencies match the expected probabilities
        System.out.println("Checking frequencies...");
        assertEquals(10 / totalWeight, (countA / numIterations), 0.1);
        assertEquals(5 / totalWeight, (countB / numIterations), 0.1);
        assertEquals(1 / totalWeight, (countC / numIterations), 0.1);
        assertEquals((50 / totalWeight) * 90 / 100, (countD / numIterations), 0.1);
        assertEquals((50 / totalWeight) * 10 / 100, (countE / numIterations), 0.1);
    }

    @Test
    @DisplayName("Element Chances Are Calculated Accurately")
    void testElementChancesAreCalculatedAccurately() {
        weighedList.clear();
        // Add categories and elements with weights
        // Add realistic loot table data with actual game items and frequencies
        // Common items (high frequency)
        weighedList.add(500000, 1, "stick");
        weighedList.add(300000, 1, "oak_sapling");

        // Uncommon materials
        weighedList.add(40000, 1, "diamond");
        weighedList.add(40000, 1, "gold_ingot");
        weighedList.add(40000, 1, "leather");
        weighedList.add(40000, 1, "emerald");
        weighedList.add(40000, 1, "netherite_ingot");

        // Progression items
        weighedList.add(20000, 1, "progression:tree_bark");

        // Weapons and tools
        weighedList.add(5000, 1, "core:booster_sword");
        weighedList.add(5000, 1, "core:booster_axe");
        weighedList.add(5000, 1, "core:power_sword");
        weighedList.add(5000, 1, "core:power_axe");
        weighedList.add(5000, 1, "core:ancient_sword");
        weighedList.add(5000, 1, "core:ancient_axe");

        // T1 Runes
        weighedList.add(5000, 1, "champions:power_rune_t1");
        weighedList.add(5000, 1, "champions:unbreaking_rune_t1");
        weighedList.add(5000, 1, "champions:mitigation_rune_t1");
        weighedList.add(5000, 1, "champions:fortune_rune_t1");
        weighedList.add(5000, 1, "champions:insight_rune_t1");
        weighedList.add(5000, 1, "champions:reinforcing_rune_t1");

        // T2 Runes
        weighedList.add(3000, 1, "champions:power_rune_t2");
        weighedList.add(3000, 1, "champions:unbreaking_rune_t2");
        weighedList.add(3000, 1, "champions:mitigation_rune_t2");
        weighedList.add(3000, 1, "champions:fortune_rune_t2");
        weighedList.add(3000, 1, "champions:insight_rune_t2");
        weighedList.add(3000, 1, "champions:conquering_rune_t2");
        weighedList.add(3000, 1, "champions:reinforcing_rune_t2");

        // T3 Runes (rare)
        weighedList.add(1000, 1, "champions:power_rune_t3");
        weighedList.add(1000, 1, "champions:scorching_rune_t3");
        weighedList.add(1000, 1, "champions:frost_rune_t3");
        weighedList.add(1000, 1, "champions:unbreaking_rune_t3");
        weighedList.add(1000, 1, "champions:mitigation_rune_t3");
        weighedList.add(1000, 1, "champions:fortune_rune_t3");
        weighedList.add(1000, 1, "champions:insight_rune_t3");
        weighedList.add(1000, 1, "champions:conqeuring_rune_t3");
        weighedList.add(1000, 1, "champions:reinforcing_rune_t3");

        // Special tokens
        weighedList.add(400, 1, "dungeons:dungeontoken");

        // T4 Runes (very rare)
        weighedList.add(100, 1, "champions:haste_rune_t4");
        weighedList.add(100, 1, "champions:alacrity_rune_t4");
        weighedList.add(100, 1, "champions:unbreaking_rune_t4");
        weighedList.add(100, 1, "champions:frost_rune_t4");
        weighedList.add(100, 1, "champions:scorching_rune_t4");

        // Extremely rare pet token
        weighedList.add(5, 1, "store:brown_squirrel_pet_token");

        System.out.println(weighedList.getAbsoluteElementChances().toString());
        weighedList.getAbsoluteElementChances().forEach((element, chance) -> {
            System.out.println(element + ": " + (chance * 100));
        });

        //test total categoryWeights
        assertEquals(869505, weighedList.getTotalCategoryWeights());
        assertEquals(1, weighedList.getAbsoluteElementChances().values().stream().mapToDouble(Float::doubleValue).sum(), 0.0001);
    }


    @Test
    @DisplayName("Roll table infinitely until all unique items are received")
    void testRollTableUntilAllUniqueItemsReceived() {
        weighedList.clear();

        // Add realistic loot table data with actual game items and frequencies
        // Common items (high frequency)
        weighedList.add(500000, 1, "stick");
        weighedList.add(300000, 1, "oak_sapling");

        // Uncommon materials
        weighedList.add(40000, 1, "diamond");
        weighedList.add(40000, 1, "gold_ingot");
        weighedList.add(40000, 1, "leather");
        weighedList.add(40000, 1, "emerald");
        weighedList.add(40000, 1, "netherite_ingot");

        // Progression items
        weighedList.add(20000, 1, "progression:tree_bark");

        // Weapons and tools
        weighedList.add(5000, 1, "core:booster_sword");
        weighedList.add(5000, 1, "core:booster_axe");
        weighedList.add(5000, 1, "core:power_sword");
        weighedList.add(5000, 1, "core:power_axe");
        weighedList.add(5000, 1, "core:ancient_sword");
        weighedList.add(5000, 1, "core:ancient_axe");

        // T1 Runes
        weighedList.add(5000, 1, "champions:power_rune_t1");
        weighedList.add(5000, 1, "champions:unbreaking_rune_t1");
        weighedList.add(5000, 1, "champions:mitigation_rune_t1");
        weighedList.add(5000, 1, "champions:fortune_rune_t1");
        weighedList.add(5000, 1, "champions:insight_rune_t1");
        weighedList.add(5000, 1, "champions:reinforcing_rune_t1");

        // T2 Runes
        weighedList.add(3000, 1, "champions:power_rune_t2");
        weighedList.add(3000, 1, "champions:unbreaking_rune_t2");
        weighedList.add(3000, 1, "champions:mitigation_rune_t2");
        weighedList.add(3000, 1, "champions:fortune_rune_t2");
        weighedList.add(3000, 1, "champions:insight_rune_t2");
        weighedList.add(3000, 1, "champions:conquering_rune_t2");
        weighedList.add(3000, 1, "champions:reinforcing_rune_t2");

        // T3 Runes (rare)
        weighedList.add(1000, 1, "champions:power_rune_t3");
        weighedList.add(1000, 1, "champions:scorching_rune_t3");
        weighedList.add(1000, 1, "champions:frost_rune_t3");
        weighedList.add(1000, 1, "champions:unbreaking_rune_t3");
        weighedList.add(1000, 1, "champions:mitigation_rune_t3");
        weighedList.add(1000, 1, "champions:fortune_rune_t3");
        weighedList.add(1000, 1, "champions:insight_rune_t3");
        weighedList.add(1000, 1, "champions:conqeuring_rune_t3");
        weighedList.add(1000, 1, "champions:reinforcing_rune_t3");

        // Special tokens
        weighedList.add(400, 1, "dungeons:dungeontoken");

        // T4 Runes (very rare)
        weighedList.add(100, 1, "champions:haste_rune_t4");
        weighedList.add(100, 1, "champions:alacrity_rune_t4");
        weighedList.add(100, 1, "champions:unbreaking_rune_t4");
        weighedList.add(100, 1, "champions:frost_rune_t4");
        weighedList.add(100, 1, "champions:scorching_rune_t4");

        // Extremely rare pet token
        weighedList.add(5, 1, "store:brown_squirrel_pet_token");


        // Get all unique items that should be obtainable
        Set<String> expectedItems = new HashSet<>(weighedList.getElements());
        Set<String> receivedItems = new HashSet<>();

        int rollCount = 0;
        int maxRolls = 1_000_000; // Safety limit to prevent infinite loops in case of bugs

        System.out.println("Starting infinite roll test...");
        System.out.println("Expected unique items: " + expectedItems.size());
        System.out.println("Items: " + expectedItems);

        // Roll until we get all unique items or hit the safety limit
        while (receivedItems.size() < expectedItems.size() && rollCount < maxRolls) {
            String rolledItem = weighedList.random();
            assertNotNull(rolledItem, "Random roll should never return null for non-empty list");

            // Track if this is a new item
            boolean isNewItem = receivedItems.add(rolledItem);
            rollCount++;

            if (isNewItem) {
                System.out.println("Roll #" + rollCount + ": Found new item '" + rolledItem + "' (" + receivedItems.size() + "/" + expectedItems.size() + ")");
            }

            // Progress updates for very rare items
            if (rollCount % 10000 == 0) {
                System.out.println("Progress: " + rollCount + " rolls, " + receivedItems.size() + "/" + expectedItems.size() + " unique items found");
            }
        }

        System.out.println("Test completed after " + rollCount + " rolls");
        System.out.println("Received items: " + receivedItems);

        // Verify we didn't hit the safety limit
        assertTrue(rollCount < maxRolls,
                "Test hit maximum roll limit (" + maxRolls + ") without finding all items. " +
                        "Found " + receivedItems.size() + "/" + expectedItems.size() + " items.");

        // Verify all expected items were found
        assertEquals(expectedItems.size(), receivedItems.size(),
                "Should have found all unique items");
        assertEquals(expectedItems, receivedItems,
                "Received items should match expected items exactly");

        // Verify no unexpected items were received
        for (String item : receivedItems) {
            assertTrue(expectedItems.contains(item),
                    "Received unexpected item: " + item);
        }

        System.out.println("✓ Successfully found all " + expectedItems.size() + " unique items in " + rollCount + " rolls");
    }
}