package me.mykindos.betterpvp.progression.progression.perks;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerStopFishingEvent;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.tree.fishing.fish.FishType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;

@BPvPListener
@Singleton
@Slf4j
public class DropMultiplierFishingPerk implements Listener, ProgressionPerk, DropMultiplier {

    @Config(path = "fishing.perks.drop-multiplier.minLevel", defaultValue = "0")
    @Inject
    int minLevel;

    @Config(path = "fishing.perks.drop-multiplier.maxLevel", defaultValue = "1000")
    @Inject
    int maxLevel;

    @Config(path = "fishing.perks.drop-multiplier.increasePerLevel", defaultValue = "0.25")
    @Inject
    double increasePerLevel;

    @Inject
    private Progression progression;

    @Inject
    private Fishing fishing;


    @Override
    public String getName() {
        return "Drop Multiplier Fishing";
    }

    @Override
    public Class<? extends ProgressionTree>[] acceptedTrees() {
        return new Class[] {
                Fishing.class
        };
    }

    @Override
    public boolean canUse(Player player, ProgressionData<?> data) {
        return minLevel <= data.getLevel() && data.getLevel() <= maxLevel;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCatch(PlayerStopFishingEvent event) {
        if (event.getReason() != PlayerStopFishingEvent.FishingResult.CATCH) return;
        if (event.getLoot() instanceof Fish loot) {
            Player player = event.getPlayer();
            fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
                if (hasPerk) {
                    int extraDrops = getMultiplier(fishing.getLevel(player));
                    Location playerLocation = player.getLocation();
                    for (int i = 0; i < extraDrops; i++) {
                        playerLocation.getWorld().dropItemNaturally(playerLocation, loot.getFishBucket());
                    }
                }
            }).exceptionally(throwable -> {
                log.error("Failed to check if player " + event.getPlayer().getName() + " has perk " + getName(), throwable);
                return null;
            });
        }



    }
}