package me.mykindos.betterpvp.clans.display;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarType;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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
        builder.addComponent(lineDrawable -> {
                    if (hasClan(player)) {
                        lineDrawable.drawLine(Component.text("Clan", NamedTextColor.YELLOW, TextDecoration.BOLD));
                        lineDrawable.drawLine(Component.text(clanManager.getClanByPlayer(player).get().getName(), ClanRelation.SELF.getPrimary()));
                        lineDrawable.drawLine(empty());
                    }
                })
                .addStaticLine(Component.text("Coins", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .addDynamicLine(() -> {
                    final int coins = (int) gamer.getProperty(GamerProperty.BALANCE).orElse(0);
                    return Component.text(UtilFormat.formatNumber(coins), NamedTextColor.GOLD);
                })
                .addBlankLine()
                .addStaticLine(Component.text("Territory", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .addDynamicLine(() -> {
                    final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(player.getLocation());

                    if (clanOptional.isEmpty() || clanOptional.get().getTerritory().isEmpty()) {
                        return Component.text("Wilderness", NamedTextColor.GRAY);
                    } else {
                        final Clan self = this.clanManager.getClanByPlayer(player).orElse(null);
                        Clan clan = clanOptional.get();
                        TextComponent text = Component.text(clan.getName(), clanManager.getRelation(self, clan).getPrimary());
                        if (clan.isAdmin() && clan.isSafe()) {
                            text = text.append(Component.text(" (", NamedTextColor.WHITE).append(Component.text("Safe", NamedTextColor.AQUA).append(Component.text(")", NamedTextColor.WHITE))));
                        }

                        return text;
                    }
                });


    }

    public boolean isEnabled() {
        return this.clans.getConfig().getOrSaveBoolean("server.sidebar.enabled", true);
    }

    private boolean hasClan(Player player) {
        return this.clanManager.getClanByPlayer(player).isPresent();
    }


}
