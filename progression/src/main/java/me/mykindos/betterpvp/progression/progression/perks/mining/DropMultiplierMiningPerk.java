package me.mykindos.betterpvp.progression.progression.perks.mining;


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
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;

import java.util.ArrayList;
import java.util.List;

@BPvPListener
@Singleton
@Slf4j
public class DropMultiplierMiningPerk implements Listener, ProgressionPerk, ChanceHandler {

    @Config(path = "mining.perks.drop-multiplier.enabled", defaultValue = "true")
    @Inject
    private boolean enabled;

    @Config(path = "mining.perks.drop-multiplier.minLevel", defaultValue = "0")
    @Inject
    int minLevel;

    @Config(path = "mining.perks.drop-multiplier.maxLevel", defaultValue = "1000")
    @Inject
    int maxLevel;

    @Config(path = "mining.perks.drop-multiplier.increasePerLevel", defaultValue = "0.25")
    @Inject
    double increasePerLevel;

    @Inject
    private Progression progression;

    @Inject
    private Mining mining;


    @Override
    public String getName() {
        return "Drop Chance Multiplier";
    }

    @Override
    public List<String> getDescription(Player player, ProgressionData<?> data) {
        List<String> description = new ArrayList<>(List.of(
                "Increase the amount of items dropped while mining by",
                "<stat>" + increasePerLevel + "%</stat> per Mining Level.",
                "Every 100%, you have a guaranteed drop, with the remainder being",
                "the chance to get another drop."
        ));
        if (canUse(player, data)) {
            description.add("Currently increases your chances by <val>" + data.getLevel() * increasePerLevel + "%</val>");
        }
        return description;
    }

    @Override
    public Class<? extends ProgressionTree>[] acceptedTrees() {
        return new Class[] {
                Mining.class
        };
    }

    @Override
    public boolean canUse(Player player, ProgressionData<?> data) {
        return minLevel <= data.getLevel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockDropItemEvent event) {
        if (!enabled) return;
        if(!mining.isEnabled()) return;
        Player player = event.getPlayer();
        if (mining.getMiningService().getExperience(event.getBlockState().getType()) <= 0) return;
        mining.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
            if (hasPerk) {
                mining.getLevel(player).whenComplete((level, throwable1) -> {
                    if (level > maxLevel) level = maxLevel;
                    level = level - minLevel;
                    int drops = getChance(level * increasePerLevel);
                    if (drops == 0 || event.getItems().isEmpty()) return;
                    event.getItems().get(0).getItemStack().add(drops - 1);
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