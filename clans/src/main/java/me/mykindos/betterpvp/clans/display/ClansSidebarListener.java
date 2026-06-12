package me.mykindos.betterpvp.clans.display;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarType;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

import static net.kyori.adventure.text.Component.empty;

@BPvPListener
@Singleton
public class ClansSidebarListener implements Listener {

    private final Clans clans;
    private final ClanManager clanManager;

    @Inject
    private ClansSidebarListener(Clans clans, ClanManager clanManager) {

        this.clans = clans;
        this.clanManager = clanManager;

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSidebar(SidebarBuildEvent event) {
        if (event.getSidebarType() != SidebarType.GENERAL) {
            return;
        }

        Gamer gamer = event.getGamer();
        Player player = gamer.getPlayer();
        if (player == null) {
            event.getSidebar().close();
            return;
        }

        SidebarComponent.Builder builder = event.getBuilder();
        builder.addBlankLine()
                .addComponent(lineDrawable -> {
                    if (!hasClan(player)) {
                        return;
                    }

                    Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

                    // Clan
                    lineDrawable.drawLine(empty().append(Translations.component("clans.sidebar.clan-header").color(TextColor.color(0xFAB95B)).decorate(TextDecoration.BOLD)));
                    lineDrawable.drawLine(empty()
                            .append(Component.text("<glyph:shield_icon_2>", NamedTextColor.GRAY))
                            .appendSpace()
                            .append(Translations.component("clans.sidebar.clan").color(TextColor.color(0xFAEB92)))
                            .appendSpace()
                            .append(Component.text(clan.getName(), ClanRelation.SELF.getPrimary())));

                    // Energy
                    lineDrawable.drawLine(empty()
                            .append(Component.text("<glyph:hourglass_icon>", NamedTextColor.GRAY))
                            .appendSpace()
                            .append(Translations.component("clans.sidebar.energy").color(TextColor.color(0xFAEB92)))
                            .appendSpace()
                            .append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.GREEN)));
                    lineDrawable.drawLine(empty());
                })
                .addStaticLine(Translations.component("clans.sidebar.info-header").color(TextColor.color(0xFAB95B)).decorate(TextDecoration.BOLD))
                .addDynamicLine(() -> {
                    final int coins = (int) gamer.getProperty(GamerProperty.BALANCE).orElse(0);
                    final TextComponent coinsText = Component.text(UtilFormat.formatNumber(coins), NamedTextColor.GOLD);
                    return empty()
                            .append(Component.text("<glyph:coins_icon>"))
                            .appendSpace()
                            .append(Translations.component("clans.sidebar.coins").color(TextColor.color(0xFAEB92)))
                            .appendSpace()
                            .append(coinsText);
                })
                .addDynamicLine(() -> {
                    final Zone zone = this.clanManager.getZoneManager().getZoneAt(player.getLocation());

                    final Component emoji;
                    final Component territory;
                    if (zone == null) {
                        emoji = Component.text("<glyph:floating_island_icon>", NamedTextColor.WHITE);
                        territory = Translations.component("clans.sidebar.wilderness").color(NamedTextColor.GRAY);
                    } else if (zone.hasTag(Zones.SAFE)) {
                        emoji = Component.text("<glyph:shield_icon>", NamedTextColor.WHITE);
                        territory = zone.getDisplayName().applyFallbackStyle(ClanRelation.SAFE.getPrimary());
                    } else if (zone.hasTag(ClanZones.TERRITORY)) {
                        final Clan self = this.clanManager.getClanByPlayer(player).orElse(null);
                        final Clan owner = this.clanManager.getClanByLocation(player.getLocation()).orElse(null);
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

                    return empty()
                            .append(emoji)
                            .appendSpace()
                            .append(Translations.component("clans.sidebar.territory").color(TextColor.color(0xFAEB92)))
                            .appendSpace()
                            .append(territory);
                })
                .addBlankLine();
    }

    public boolean isEnabled() {
        return this.clans.getConfig().getOrSaveBoolean("server.sidebar.enabled", true);
    }

    private boolean hasClan(Player player) {
        return this.clanManager.getClanByPlayer(player).isPresent();
    }


}
