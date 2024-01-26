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
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerThrowBaitEvent;
import me.mykindos.betterpvp.progression.tree.fishing.model.Bait;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@Slf4j
public class LongerBaitFishingPerk implements Listener, ProgressionPerk {
    @Config(path = "fishing.perks.longer-bait.enabled", defaultValue = "true")
    @Inject
    private boolean enabled;

    @Config(path = "fishing.perks.longer-bait.minLevel", defaultValue = "0")
    @Inject
    int minLevel;

    @Config(path = "fishing.perks.longer-bait.maxLevel", defaultValue = "1000")
    @Inject
    int maxLevel;

    @Config(path = "fishing.perks.longer-bait.increasePerLevel", defaultValue = "0.0025")
    @Inject
    double increasePerLevel;

    @Inject
    private Progression progression;

    @Inject
    private Fishing fishing;

    @Override
    public String getName() {
        return "Bait Duration Increase";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Increases the duration of baits you throw by",
                "<stat>" + increasePerLevel * 100 + "%</stat> per Fishing level.",
                "Currently increases duration of baits by <val>" + getPercentage(level) * 100 + "%</val>",
        };
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

    private double getPercentage(int level) {
        return (level * increasePerLevel / 100);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBait(PlayerThrowBaitEvent event) {
        if (!enabled) return;
        if(!fishing.isEnabled()) return;
        Player player = event.getPlayer();
        fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
            if (hasPerk) {
                fishing.getLevel(player).whenComplete((level, throwable1) -> {
                    if (level > maxLevel) level = maxLevel;
                    //make leveling more intuitive
                    level = level - minLevel;
                    Bait bait = event.getBait();
                    double multiplier = (1f + getPercentage(level));
                    bait.setDurationTicks((long) (bait.getDurationTicks() * multiplier));
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