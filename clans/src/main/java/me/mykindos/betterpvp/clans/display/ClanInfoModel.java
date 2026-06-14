package me.mykindos.betterpvp.clans.display;

import lombok.Value;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Presentation-neutral source of truth for the clan info readout. Both the scoreboard
 * {@link ClansSidebarListener} and the boss-bar {@link ClanHudInfo} consume this so the
 * non-trivial territory/relation resolution lives in exactly one place.
 */
public final class ClanInfoModel {

    private ClanInfoModel() {
    }

    /** The resolved territory display: the icon glyph and the coloured territory name. */
    @Value
    public static class Territory {
        Component emoji;
        Component name;
    }

    /**
     * Resolves the territory the player is standing in into an icon + coloured name, mirroring the
     * wilderness / safe-zone / clan-territory rules used across the clans UI.
     */
    public static Territory territory(ClanManager clanManager, Player player) {
        final Zone zone = clanManager.getZoneManager().getZoneAt(player.getLocation());

        final Component emoji;
        final Component territory;
        if (zone == null) {
            emoji = Component.text("<glyph:floating_island_icon>", NamedTextColor.WHITE);
            territory = Translations.component("clans.sidebar.wilderness").color(NamedTextColor.GRAY);
        } else if (zone.hasTag(Zones.SAFE)) {
            emoji = Component.text("<glyph:shield_icon>", NamedTextColor.WHITE);
            territory = zone.getDisplayName().applyFallbackStyle(ClanRelation.SAFE.getPrimary());
        } else if (zone.hasTag(ClanZones.TERRITORY)) {
            final Clan self = clanManager.getClanByPlayer(player).orElse(null);
            final Clan owner = clanManager.getClanByLocation(player.getLocation()).orElse(null);
            final ClanRelation relation = clanManager.getRelation(self, owner);

            emoji = switch (relation) {
                case PILLAGE, ENEMY, NEUTRAL -> Component.text("<glyph:sword_icon>", NamedTextColor.WHITE);
                case SAFE, SELF, ALLY, ALLY_TRUST -> Component.text("<glyph:shield_icon>", NamedTextColor.WHITE);
            };
            territory = zone.getDisplayName().applyFallbackStyle(relation.getPrimary());
        } else {
            emoji = Component.text("<glyph:floating_island_icon>", NamedTextColor.WHITE);
            territory = zone.getDisplayName().applyFallbackStyle(NamedTextColor.GRAY);
        }

        return new Territory(emoji, territory);
    }
}
