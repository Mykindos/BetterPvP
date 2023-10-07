package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerStartFishingEvent;
import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@Singleton
@Slf4j
public class BaseFishingPerk implements Listener, ProgressionPerk {

    @Config(path = "fishing.base-fishing-perk.level", defaultValue = "500")
    @Inject(optional = true)
    private int requiredLevel;

    @Inject(optional = true)
    private ClanManager manager;

    @Inject(optional = true)
    private Progression progression;

    @Inject(optional = true)
    private Fishing fishing;

    @Override
    public String getName() {
        return "Base Fishing";
    }

    @Override
    public Class<? extends ProgressionTree>[] acceptedTrees() {
        return new Class[] {
                Fishing.class
        };
    }

    @Override
    public boolean canUse(Player player, ProgressionData<?> data) {
        return data.getLevel() > requiredLevel;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerStartFishingEvent event) {
        final FishHook hook = event.getPlayer().getFishHook();
        if (hook == null || !hook.isValid()) {
            return;
        }

        final Location fishingLocation = hook.getLocation();
        final Player player = event.getPlayer();
        final Optional<Clan> clan = manager.getClanByLocation(fishingLocation);
        fishing.hasPerk(event.getPlayer(), getClass()).whenComplete((hasPerk, throwable) -> {
            if (hasPerk) {
                return; // Don't cancel if they have the perk, and they're in their base
            }

            // Otherwise, cancel if they're not in fields or lake
            if (clan.map(c -> c.getName().equalsIgnoreCase("Fields") || c.getName().equalsIgnoreCase("Lake")).orElse(false)) {
                return;
            }

            UtilServer.runTask(progression, hook::remove);
            UtilMessage.message(player, "Fishing", "<red>You cannot fish in this area!");
        }).exceptionally(throwable -> {
            log.error("Failed to check if player " + player.getName() + " has perk " + getName(), throwable);
            return null;
        });
    }
}
