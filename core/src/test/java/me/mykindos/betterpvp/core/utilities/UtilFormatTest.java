package me.mykindos.betterpvp.core.utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UtilFormatTest {

    @Test
    @DisplayName("Hash with Salt Test")
    void testHashWithSalt() {
        String hostAddress1 = "192.168.0.1";
        String pepper1 = "testPepper";
        String expectedHash1 = "d195ab410e999d1f560e8cf5337831313112f8dac67397d6ac897bfab6a4d799";
        Assertions.assertEquals(expectedHash1, UtilFormat.hashWithSalt(hostAddress1, pepper1));

        String hostAddress2 = "10.0.0.1";
        String pepper2 = "anotherPepper";
        String expectedHash2 = "5163e71cee932a6ffa9e3bc5f68cbef638d50760a0f5f04fb4c4dd1d2727567d";
        Assertions.assertEquals(expectedHash2, UtilFormat.hashWithSalt(hostAddress2, pepper2));

        String hostAddress3 = "127.0.0.1";
        String pepper3 = "examplePepper";
        String expectedHash3 = "4df64d933cfe06c214a1d8dcb5d9732fa8b4a7fca3cc25d55c6f6cc5e355832f";
        Assertions.assertEquals(expectedHash3, UtilFormat.hashWithSalt(hostAddress3, pepper3));

        Assertions.assertThrows(RuntimeException.class, () ->
                UtilFormat.hashWithSalt(null, "pepper"), "Expected exception for null hostAddress but didn't get one.");

        Assertions.assertThrows(RuntimeException.class, () ->
                UtilFormat.hashWithSalt("hostAddress", null), "Expected exception for null pepper but didn't get one.");
    }

    @Test
    @DisplayName("formatNumber Int")
    void formatIntNumber() {
        String formattedString = UtilFormat.formatNumber((int) 5);
        Assertions.assertEquals("5", formattedString);
    }

    @Test
    @DisplayName("formatNumber double")
    void testFormatDoubleNumber() {
        String formattedString1 = UtilFormat.formatNumber(5.554123);
        Assertions.assertEquals("5.55", formattedString1);

        String formattedString2 = UtilFormat.formatNumber(6.69823);
        Assertions.assertEquals("6.7", formattedString2);

        String formattedString3 = UtilFormat.formatNumber(8.1);
        Assertions.assertEquals("8.1", formattedString3);

        String formattedString4 = UtilFormat.formatNumber(20.166);
        Assertions.assertEquals("20.17", formattedString4);
    }

    @Test
    @DisplayName("formatNumber double places")
    void testFormatDoublePlaceNumber() {
        String formattedString1 = UtilFormat.formatNumber(5.554123, 4);
        Assertions.assertEquals("5.5541", formattedString1);

        String formattedString2 = UtilFormat.formatNumber(6.69823, 3);
        Assertions.assertEquals("6.698", formattedString2);

        String formattedString3 = UtilFormat.formatNumber(8.7, 0);
        Assertions.assertEquals("9", formattedString3);

        String formattedString4 = UtilFormat.formatNumber(20.166, 1);
        Assertions.assertEquals("20.2", formattedString4);
    }

    @Test
    @DisplayName("formatNumber double places trailingZeros")
    void testFormatDoublePlaceTrailingNumber() {
        String formattedString1 = UtilFormat.formatNumber(5.554123, 4, true);
        Assertions.assertEquals("5.5541", formattedString1);

        String formattedString2 = UtilFormat.formatNumber(6.69823, 3, true);
        Assertions.assertEquals("6.698", formattedString2);

        String formattedString3 = UtilFormat.formatNumber(8.796, 2, true);
        Assertions.assertEquals("8.80", formattedString3);

        String formattedString4 = UtilFormat.formatNumber(20.166, 1, false);
        Assertions.assertEquals("20.2", formattedString4);
    }

    /*@Test()
    @DisplayName("Clean String test")
    void cleanString() {
        String formattedString1 = UtilFormat.cleanString("this_is_a_dirty_string");
        Assertions.assertEquals("This Is A Dirty String", formattedString1);
    }*/

    @Test
    @DisplayName("Lunar Spoof String")
    void spoofNameForLunar() {
        String spoofedString = UtilFormat.spoofNameForLunar("Test");
        Assertions.assertEquals("T\u200Cest", spoofedString);
    }

    @Test
    @DisplayName("Roman Numeral Test")
    void getRomanNumeral() {
        String one = UtilFormat.getRomanNumeral(1);
        Assertions.assertEquals("I", one);

        String five = UtilFormat.getRomanNumeral(5);
        Assertions.assertEquals("V", five);

        String nine = UtilFormat.getRomanNumeral(9);
        Assertions.assertEquals("IX", nine);
    }


    @Test
    @DisplayName("Indefinite Article Test")
    void getIndefiniteArticle() {
        String a = UtilFormat.getIndefiniteArticle("cat");
        Assertions.assertEquals("a", a);

        String an = UtilFormat.getIndefiniteArticle("amber");
        Assertions.assertEquals("an", an);
    }
}