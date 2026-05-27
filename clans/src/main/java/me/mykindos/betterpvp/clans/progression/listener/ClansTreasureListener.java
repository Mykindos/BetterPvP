package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.FishingTreasureChanceDropTableEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.WoodcuttingTreasureChanceDropTableEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class ClansTreasureListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public ClansTreasureListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onFishingTreasureCalculation(FishingTreasureChanceDropTableEvent event) {
        Optional<Clan> locationClan = clanManager.getClanByLocation(event.getLocation());
        Optional<Clan> standingClan = clanManager.getClanByLocation(event.getPlayer().getLocation());

        if (locationClan.map(clan -> clan.getName().equalsIgnoreCase("Fields")).orElse(false)
                || standingClan.map(clan -> clan.getName().equalsIgnoreCase("Fields")).orElse(false)) {
            return;
        }


        if (locationClan.isPresent() || standingClan.isPresent()) {
            event.setLootTableId("fishing_treasure_chance_in_clan_territory");
        }
    }

    @EventHandler
    public void onWoodcuttingTreasureCalculation(WoodcuttingTreasureChanceDropTableEvent event) {
        Optional<Clan> locationClan = clanManager.getClanByLocation(event.getLocation());
        Optional<Clan> standingClan = clanManager.getClanByLocation(event.getPlayer().getLocation());

        if (locationClan.map(clan -> clan.getName().equalsIgnoreCase("Fields")).orElse(false)
                || standingClan.map(clan -> clan.getName().equalsIgnoreCase("Fields")).orElse(false)) {
            return;
        }

        Optional<Clan> playerClan = clanManager.getClanByPlayer(event.getPlayer());
        if (playerClan.isEmpty()) return;
        Clan ownClan = playerClan.get();

        if (locationClan.map(ownClan::equals).orElse(false) || standingClan.map(ownClan::equals).orElse(false)) {
            event.setLootTableId("woodcutting_treasure_chance_in_clan_territory");
        }
    }
}
