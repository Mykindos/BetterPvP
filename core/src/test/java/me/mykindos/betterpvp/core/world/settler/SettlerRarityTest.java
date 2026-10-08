package me.mykindos.betterpvp.core.world.settler;

import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlerRarityTest {

    @Test
    void ac23_raritiesRunCommonToLegendaryInWhiteGreenBlueAndGold() {
        assertEquals(List.of(SettlerRarity.COMMON, SettlerRarity.UNCOMMON, SettlerRarity.RARE, SettlerRarity.LEGENDARY),
                List.of(SettlerRarity.values()));
        assertEquals(List.of(NamedTextColor.WHITE, NamedTextColor.GREEN, NamedTextColor.BLUE, NamedTextColor.GOLD),
                List.of(SettlerRarity.values()).stream().map(SettlerRarity::getColor).toList());
        assertEquals(NamedTextColor.BLUE, SettlerRarity.RARE.displayName().color());
        assertTrue(SettlerRarity.RARE.isAtLeast(SettlerRarity.UNCOMMON));
        assertFalse(SettlerRarity.UNCOMMON.isAtLeast(SettlerRarity.RARE));
    }
}
