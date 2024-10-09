package me.mykindos.betterpvp.core.utilities.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;

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
        // Add categories and elements with weights
        weighedList.add(10, 1, "A");
        weighedList.add(5, 1, "B");
        weighedList.add(1, 1, "C");
        weighedList.add(50, 90, "D");
        weighedList.add(50, 10, "E");

        System.out.println(weighedList.AbsoluteElementChances().toString());

        //test total categoryWieghts
        assertEquals(66, weighedList.getTotalCategoryWeights());
        assertEquals(1, weighedList.AbsoluteElementChances().values().stream().mapToDouble(Float::doubleValue).sum(), 0.0001);
    }
}