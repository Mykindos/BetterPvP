package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import me.mykindos.betterpvp.progression.tree.mining.event.ProgressionMiningEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;

import java.util.function.LongUnaryOperator;

@Slf4j
@Singleton
@BPvPListener
public class FieldExperienceMiningPerk implements Listener, ProgressionPerk {

    @Config(path = "mining.perks.field-xp-multiplier.minLevel", defaultValue = "0")
    @Inject(optional = true)
    int minLevel;

    @Config(path = "mining.perks.field-xp-multiplier.maxLevel", defaultValue = "1000")
    @Inject(optional = true)
    int maxLevel;

    @Config(path = "mining.perks.field-xp-multiplier.increasePerLevel", defaultValue = "0.003")
    @Inject(optional = true)
    double increasePerLevel;

    @Inject(optional = true)
    private Progression progression;

    @Inject(optional = true)
    private Mining mining;


    @Override
    public String getName() {
        return "mining-field-xp-multiplier";
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
    public void onBreak(ProgressionMiningEvent event) {
        Player player = event.getPlayer();
        if (mining.getMiningService().getExperience(event.getBlock().getType()) <= 0) return;
        mining.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
            if (hasPerk) {
                mining.getLevel(player).whenComplete((level, throwable1) -> {
                    if (level > maxLevel) level = maxLevel;
                    level = level - minLevel;
                    double increase = 1 + (level * increasePerLevel);
                    log.info(String.valueOf(increase));
                    log.info(String.valueOf((long) (50L * increase)));
                    LongUnaryOperator percentIncrease = xp -> (long) (xp * increase);
                    event.setExperienceModifier(event.getExperienceModifier().andThen(percentIncrease));
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