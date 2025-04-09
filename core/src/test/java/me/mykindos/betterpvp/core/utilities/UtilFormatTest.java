package me.mykindos.betterpvp.core.utilities;

import java.util.UUID;
import me.mykindos.betterpvp.core.Core;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class UtilFormatTest {

    private static Core plugin;
    private static ServerMock server;

    public static void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Core.class);
    }

    public static void tearDown() {
        MockBukkit.unmock();
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

    @Test
    @DisplayName("Clean String test")
    void cleanString() {
        String formattedString1 = UtilFormat.cleanString("this_is_a_dirty_string");
        Assertions.assertEquals("This Is A Dirty String", formattedString1);
    }

    @Test
    @DisplayName("Online Status Test")
    void getOnlineStatus() {
        setUp();
        PlayerMock onlinePlayer = server.addPlayer();
        UUID onlineUUID = onlinePlayer.getUniqueId();
        UUID offlineUUID = UUID.randomUUID();

        String onlineUUIDString = UtilFormat.getOnlineStatus(onlineUUID);
        String onlineStringString = UtilFormat.getOnlineStatus(onlineUUID.toString());

        Assertions.assertEquals("<green>", onlineUUIDString);
        Assertions.assertEquals("<green>", onlineStringString);

        String offlineUUIDString = UtilFormat.getOnlineStatus(offlineUUID);
        String offlineStringString = UtilFormat.getOnlineStatus(offlineUUID.toString());

        Assertions.assertEquals("<red>", offlineUUIDString);
        Assertions.assertEquals("<red>", offlineStringString);

        tearDown();
    }

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