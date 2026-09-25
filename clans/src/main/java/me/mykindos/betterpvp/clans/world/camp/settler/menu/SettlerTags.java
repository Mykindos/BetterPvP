package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The tag glyphs menus show in place of a settler's rarity, profession or specialty name. */
public final class SettlerTags {

    private static final Map<SettlerRarity, String> RARITIES = Map.of(
            SettlerRarity.COMMON, "\uE00B",
            SettlerRarity.UNCOMMON, "\uE03B",
            SettlerRarity.RARE, "\uE02F",
            SettlerRarity.LEGENDARY, "\uE01E");

    private static final Map<String, String> ROLES = Map.of(
            CampProfessions.BUILDER, "\uE006",
            CampProfessions.FARMER, "\uE017",
            CampProfessions.MASON, "\uE025",
            CampProfessions.CARPENTER, "\uE008",
            CampProfessions.SMITH, "\uE036",
            CampProfessions.LABORER, "\uE01C");

    private SettlerTags() {
    }

    public static @NotNull Component rarity(@NotNull SettlerRarity rarity) {
        return glyph(RARITIES.get(rarity));
    }

    /** The tag of profession or specialty {@code id}, if it has one. */
    public static @NotNull Optional<Component> role(@NotNull String id) {
        return Optional.ofNullable(ROLES.get(id)).map(SettlerTags::glyph);
    }

    /** {@code tags} side by side on one lore line. */
    public static @NotNull Component line(@NotNull List<Component> tags) {
        return Component.join(JoinConfiguration.spaces(), tags);
    }

    private static @NotNull Component glyph(@NotNull String glyph) {
        return Component.text(glyph, NamedTextColor.WHITE)
                .font(Key.key("betterpvp", "tags"))
                .shadowColor(ShadowColor.none());
    }
}
