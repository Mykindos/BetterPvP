package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static me.mykindos.betterpvp.clans.testing.Messages.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlerTagsTest {

    @Test
    void ac24_raritiesShowAsTheirTagGlyphs() {
        final Map<SettlerRarity, String> glyphs = Map.of(
                SettlerRarity.COMMON, "",
                SettlerRarity.UNCOMMON, "",
                SettlerRarity.RARE, "",
                SettlerRarity.LEGENDARY, "");
        glyphs.forEach((rarity, glyph) -> assertTag(SettlerTags.rarity(rarity), glyph));
    }

    @Test
    void ac24_professionsAndSpecialtiesShowAsTheirTagGlyphs() {
        final Map<String, String> glyphs = Map.of(
                CampProfessions.BUILDER, "",
                CampProfessions.FARMER, "",
                CampProfessions.MASON, "",
                CampProfessions.CARPENTER, "",
                CampProfessions.SMITH, "",
                CampProfessions.LABORER, "");
        glyphs.forEach((id, glyph) -> assertTag(SettlerTags.role(id).orElseThrow(), glyph));
    }

    @Test
    void ac24_anIdWithNoTagGetsNone() {
        assertTrue(SettlerTags.role("bard").isEmpty());
    }

    @Test
    void ac24_tagsSitSideBySideOnOneLine() {
        final Component line = SettlerTags.line(List.of(SettlerTags.rarity(SettlerRarity.RARE),
                SettlerTags.role(CampProfessions.MASON).orElseThrow()));

        assertEquals(" ", text(line));
    }

    private static void assertTag(Component tag, String glyph) {
        assertEquals(glyph, ((TextComponent) tag).content());
        assertEquals(NamedTextColor.WHITE, tag.color());
        assertEquals(Key.key("betterpvp", "tags"), tag.font());
        assertEquals(ShadowColor.none(), tag.shadowColor());
    }
}
