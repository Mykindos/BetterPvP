package me.mykindos.betterpvp.clans.display;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.sidebar.Sidebar;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class ClansSidebar extends Sidebar {

    private final Clans clans;
    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    private ClansSidebar(Clans clans, ClanManager clanManager, ClientManager clientManager) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.clientManager = clientManager;

        // Clan
        this.addConditionalLine(player -> Component.text("Clan", NamedTextColor.YELLOW, TextDecoration.BOLD), this::hasClan);
        this.addConditionalLine(player -> {
            final Clan clan = this.clanManager.getClanByPlayer(player).orElseThrow();
            return Component.text(clan.getName(), ClanRelation.SELF.getPrimary());
        }, this::hasClan);
        this.addConditionalLine(player -> Component.empty(), this::hasClan);

        // Coins
        this.addLine(Component.text("Coins", NamedTextColor.YELLOW, TextDecoration.BOLD));
        this.addUpdatableLine(player -> {
            final Client client = this.clientManager.search().online(player);
            final Gamer gamer = client.getGamer();
            final int coins = (int) gamer.getProperty(GamerProperty.BALANCE).orElse(0);
            return Component.text(UtilFormat.formatNumber(coins), NamedTextColor.GOLD);
        });
        this.addBlankLine();

        // Territory
        this.addLine(Component.text("Territory", NamedTextColor.YELLOW, TextDecoration.BOLD));
        this.addUpdatableLine(player -> {
            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(player.getLocation());

            if (clanOptional.isEmpty() || clanOptional.get().getTerritory().isEmpty()) {
                return Component.text("Wilderness", NamedTextColor.GRAY);
            } else {
                final Clan self = this.clanManager.getClanByPlayer(player).orElse(null);
                Clan clan = clanOptional.get();
                TextComponent text = Component.text(clan.getName(), clanManager.getRelation(self, clan).getPrimary());
                if(clan.isAdmin() && clan.isSafe()) {
                    text = text.append(Component.text(" (", NamedTextColor.WHITE).append(Component.text("Safe", NamedTextColor.AQUA).append(Component.text(")", NamedTextColor.WHITE))));
                }

                return text;
            }
        });

        reload();
    }

    public boolean isEnabled() {
        return this.clans.getConfig().getOrSaveBoolean("server.sidebar.enabled", true);
    }

    private boolean hasClan(Player player) {
        return this.clanManager.getClanByPlayer(player).isPresent();
    }

    public void reload() {
        final String title = this.clans.getConfig().getOrSaveString("server.sidebar.title", "BetterPvP");
        this.setTitle(Sidebar.defaultTitle("   " + title + "   "));
    }

}
