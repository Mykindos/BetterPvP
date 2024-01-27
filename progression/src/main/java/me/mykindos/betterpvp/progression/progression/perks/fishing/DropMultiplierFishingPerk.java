package me.mykindos.betterpvp.progression.progression.perks.fishing;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.progression.perks.ChanceHandler;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerStopFishingEvent;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

@BPvPListener
@Singleton
@Slf4j
public class DropMultiplierFishingPerk implements Listener, ProgressionPerk, ChanceHandler {

    @Config(path = "fishing.perks.drop-multiplier.enabled", defaultValue = "true")
    @Inject
    private boolean enabled;

    @Config(path = "fishing.perks.drop-multiplier.minLevel", defaultValue = "0")
    @Inject
    private int minLevel;

    @Config(path = "fishing.perks.drop-multiplier.maxLevel", defaultValue = "1000")
    @Inject
    private int maxLevel;

    @Config(path = "fishing.perks.drop-multiplier.increasePerLevel", defaultValue = "0.25")
    @Inject
    private double increasePerLevel;

    @Inject
    private Progression progression;

    @Inject
    private Fishing fishing;


    @Override
    public String getName() {
        return "Drop Multiplier Fishing";
    }

    @Override
    public List<String> getDescription(Player player, ProgressionData<?> data) {
        List<String> description = new ArrayList<>(List.of(
                "TODO"
        ));
        if (canUse(player, data)) {
            description.add("Can Use");
        }
        return description;
    }

    @Override
    public Class<? extends ProgressionTree>[] acceptedTrees() {
        return new Class[] {
                Fishing.class
        };
    }

    @Override
    public boolean canUse(Player player, ProgressionData<?> data) {
        return minLevel <= data.getLevel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCatch(PlayerStopFishingEvent event) {
        if (!enabled) return;
        if(!fishing.isEnabled()) return;
        if (event.getReason() != PlayerStopFishingEvent.FishingResult.CATCH) return;
        if (event.getLoot() instanceof Fish loot) {
            Player player = event.getPlayer();
            fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
                if (hasPerk) {
                    fishing.getLevel(player).whenComplete((level, throwable1) -> {
                        //cannot increase chance over the max level
                        if (level > maxLevel) level = maxLevel;
                        int extraDrops = getChance((level - minLevel) * increasePerLevel);
                        Location playerLocation = player.getLocation();
                        for (int i = 0; i < extraDrops; i++) {
                            playerLocation.getWorld().dropItemNaturally(playerLocation, loot.getFishBucket());
                        }
                    }).exceptionally(throwable1 -> {
                        log.error("Failed to check if player " + event.getPlayer().getName() + " has a level ", throwable);
                        return null;
                    });
                }
            }).exceptionally(throwable -> {
                log.error("Failed to check if player " + event.getPlayer().getName() + " has perk " + getName(), throwable);
                return null;
            });
        }
    }
}