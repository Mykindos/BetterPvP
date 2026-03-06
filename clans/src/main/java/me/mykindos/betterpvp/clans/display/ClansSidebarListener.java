package me.mykindos.betterpvp.clans.display;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarType;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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
                    lineDrawable.drawLine(empty().append(Component.text("Clan", TextColor.color(0xFAB95B), TextDecoration.BOLD)));
                    lineDrawable.drawLine(empty()
                            .append(Component.text("<glyph:shield_icon_2>", NamedTextColor.GRAY))
                            .appendSpace()
                            .append(Component.text("Clan:", TextColor.color(0xFAEB92)))
                            .appendSpace()
                            .append(Component.text(clan.getName(), ClanRelation.SELF.getPrimary())));

                    // Energy
                    lineDrawable.drawLine(empty()
                            .append(Component.text("<glyph:hourglass_icon>", NamedTextColor.GRAY))
                            .appendSpace()
                            .append(Component.text("Energy:", TextColor.color(0xFAEB92)))
                            .appendSpace()
                            .append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.GREEN)));
                    lineDrawable.drawLine(empty());
                })
                .addStaticLine(Component.text("Info", TextColor.color(0xFAB95B), TextDecoration.BOLD))
                .addDynamicLine(() -> {
                    final int coins = (int) gamer.getProperty(GamerProperty.BALANCE).orElse(0);
                    final TextComponent coinsText = Component.text(UtilFormat.formatNumber(coins), NamedTextColor.GOLD);
                    return empty()
                            .append(Component.text("<glyph:coins_icon>"))
                            .appendSpace()
                            .append(Component.text("Coins:", TextColor.color(0xFAEB92)))
                            .appendSpace()
                            .append(coinsText);
                })
                .addDynamicLine(() -> {
                    final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(player.getLocation());

                    final Component emoji;
                    final Component territory;
                    if (clanOptional.isEmpty() || clanOptional.get().getTerritory().isEmpty()) {
                        emoji = Component.text("<glyph:floating_island_icon>", NamedTextColor.WHITE);
                        territory = Component.text("Wilderness", NamedTextColor.GRAY);
                    } else {
                        final Clan self = this.clanManager.getClanByPlayer(player).orElse(null);
                        Clan clan = clanOptional.get();
                        ClanRelation relation = clanManager.getRelation(self, clan);

                        if (clan.isAdmin() && clan.isSafe()) {
                            emoji = Component.text("<glyph:shield_icon>", NamedTextColor.WHITE);
                        } else {
                            emoji = switch (relation) {
                                case PILLAGE, ENEMY, NEUTRAL -> Component.text("<glyph:sword_icon>", NamedTextColor.WHITE);
                                case SAFE, SELF, ALLY, ALLY_TRUST -> Component.text("<glyph:shield_icon>", NamedTextColor.WHITE);
                            };
                        }
                        territory = Component.text(clan.getName(), clanManager.getRelation(self, clan).getPrimary());
                    }

                    return empty()
                            .append(emoji)
                            .appendSpace()
                            .append(Component.text("Territory:", TextColor.color(0xFAEB92)))
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
