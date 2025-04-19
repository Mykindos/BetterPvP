package me.mykindos.betterpvp.core.utilities.model;

import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
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
        weighedList.add(1300, 1, "champions:ancient_sword");
        weighedList.add(1300, 1, "champions:ancient_axe");
        weighedList.add(1100, 1, "champions:conquering_rune_t2");
        weighedList.add(1100, 1, "champions:fortune_rune_t2");
        weighedList.add(1100, 1, "champions:insight_rune_t2");
        weighedList.add(1100, 1, "champions:power_rune_t2");
        weighedList.add(1100, 1, "champions:reinforcing_rune_t2");
        weighedList.add(1100, 1, "champions:mitigation_rune_t2");
        weighedList.add(1100, 1, "champions:unbreaking_rune_t2");
        weighedList.add(1000, 1, "dungeons:dungeon_token");
        weighedList.add(800, 1, "champions:conquering_rune_t3");
        weighedList.add(800, 1, "champions:frost_rune_t3");
        weighedList.add(800, 1, "champions:insight_rune_t3");
        weighedList.add(800, 1, "champions:power_rune_t3");
        weighedList.add(800, 1, "champions:reinforcing_rune_t3");
        weighedList.add(800, 1, "champions:mitigation_rune_t3");
        weighedList.add(800, 1, "champions:scorching_rune_t3");
        weighedList.add(800, 1, "champions:unbreaking_rune_t3");
        weighedList.add(200, 3, "champions:alligators_tooth");
        weighedList.add(200, 4, "champions:rake");
        weighedList.add(200, 4, "champions:runed_pickaxe");
        weighedList.add(100, 1, "champions:giants_broadsword");
        weighedList.add(100, 1, "champions:hyper_axe");
        weighedList.add(100, 1, "champions:magnetic_maul");
        weighedList.add(100, 1, "champions:thunderclap_aegis");
        weighedList.add(300, 1, "store:turkinator_hat_token");

        System.out.println(weighedList.getAbsoluteElementChances().toString());
        weighedList.getAbsoluteElementChances().forEach((element, chance) -> {
            System.out.println(element + ": " + (chance * 100));
        });

        //test total categoryWeights
        assertEquals(4800, weighedList.getTotalCategoryWeights());
        assertEquals(1, weighedList.getAbsoluteElementChances().values().stream().mapToDouble(Float::doubleValue).sum(), 0.0001);
    }
}