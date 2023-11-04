package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import me.mykindos.betterpvp.progression.tree.mining.event.ProgressionMiningEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.function.LongUnaryOperator;

@Slf4j
@Singleton
public class FieldExperienceMiningPerk implements Listener, ProgressionPerk {

    @Config(path = "mining.perks.field-xp-multiplier.minLevel", defaultValue = "0")
    @Inject(optional = true)
    int minLevel;

    @Config(path = "mining.perks.field-xp-multiplier.maxLevel", defaultValue = "1000")
    @Inject(optional = true)
    int maxLevel;

    @Config(path = "mining.perks.field-xp-multiplier.increasePerLevel", defaultValue = "0.002")
    @Inject(optional = true)
    double increasePerLevel;

    @Inject(optional = true)
    private Progression progression;

    @Inject(optional = true)
    private Mining mining;

    @Inject(optional = true)
    private ClanManager clanManager;



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

    @EventHandler
    public void onBreak(ProgressionMiningEvent event) {
        Player player = event.getPlayer();
        if (mining.getMiningService().getExperience(event.getBlock().getType()) <= 0) return;
        mining.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
            if (hasPerk) {
                if (clanManager.isFields(event.getBlock().getLocation())){
                    mining.getLevel(player).whenComplete((level, throwable1) -> {
                        if (level > maxLevel) level = maxLevel;
                        level = level - minLevel;
                        double increase = 1 + (level * increasePerLevel);
                        LongUnaryOperator percentIncrease = xp -> (long) (xp * increase);
                        event.setExperienceModifier(event.getExperienceModifier().andThen(percentIncrease));
                    }).exceptionally(throwable1 -> {
                        log.error("Failed to check if player " + event.getPlayer().getName() + " has a level ", throwable);
                        return null;
                    });
                }
            }
        }).exceptionally(throwable -> {
            log.error("Failed to check if player " + event.getPlayer().getName() + " has perk " + getName(), throwable);
            return null;
        });
    }
}
