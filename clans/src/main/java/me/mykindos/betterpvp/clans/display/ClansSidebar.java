package me.mykindos.betterpvp.clans.display;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.sidebar.impl.Sidebar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class ClansSidebar extends Sidebar {

    private final Clans clans;
    private final ClanManager clanManager;
    private final ClientManager clientManager;
    private final Sidebar sidebar;

    @Inject
    private ClansSidebar(Clans clans, ClanManager clanManager, ClientManager clientManager) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.clientManager = clientManager;
        this.sidebar = new Sidebar();

        // Clan
//        this.sidebar.addConditionalLine(player -> Component.text("Clan", NamedTextColor.YELLOW, TextDecoration.BOLD), this::hasClan);
//        this.sidebar.addConditionalLine(player -> {
//            final Clan clan = this.clanManager.getClanByPlayer(player).orElseThrow();
//            return Component.text(clan.getName(), ClanRelation.SELF.getPrimary());
//        }, this::hasClan);
//        this.sidebar.addConditionalLine(player -> Component.empty(), this::hasClan);
//
//        // Clan Energy
//        this.sidebar.addConditionalLine(player -> Component.text("Clan Energy", NamedTextColor.YELLOW, TextDecoration.BOLD), this::hasClan);
//        this.sidebar.addConditionalLine(player -> {
//            final Clan clan = this.clanManager.getClanByPlayer(player).orElseThrow();
//            return Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.GREEN);
//        }, this::hasClan);
//        this.sidebar.addConditionalLine(player -> Component.empty(), this::hasClan);
//
//        // Coins
//        this.sidebar.addLine(Component.text("Coins", NamedTextColor.YELLOW, TextDecoration.BOLD));
//        this.sidebar.addUpdatableLine(player -> {
//            final Client client = this.clientManager.search().online(player);
//            final Gamer gamer = client.getGamer();
//            final int coins = (int) gamer.getProperty(GamerProperty.BALANCE).orElse(0);
//            return Component.text(UtilFormat.formatNumber(coins), NamedTextColor.GOLD);
//        });
//        this.sidebar.addBlankLine();
//
//        // Territory
//        this.sidebar.addLine(Component.text("Territory", NamedTextColor.YELLOW, TextDecoration.BOLD));
//        this.sidebar.addUpdatableLine(player -> {
//            final Optional<Clan> clan = this.clanManager.getClanByLocation(player.getLocation());
//            if (clan.isEmpty() || clan.get().getTerritory().isEmpty()) {
//                return Component.text("Wilderness", NamedTextColor.GRAY);
//            } else {
//                final Clan self = this.clanManager.getClanByPlayer(player).orElse(null);
//                return Component.text(clan.get().getName(), clanManager.getRelation(self, clan.get()).getPrimary());
//            }
//        });
//        this.sidebar.addBlankLine();
    }

    public boolean isEnabled() {
        return this.clans.getConfig().getOrSaveBoolean("server.sidebar.enabled", true);
    }

    private boolean hasClan(Player player) {
        return this.clanManager.getClanByPlayer(player).isPresent();
    }

    public void reload() {
        final String title = this.clans.getConfig().getOrSaveString("server.sidebar.title", "BetterPvP");
        this.sidebar.setTitle(Component.empty());
        this.sidebar.addBlankLine();
        this.sidebar.addBlankLine();
    }

}
