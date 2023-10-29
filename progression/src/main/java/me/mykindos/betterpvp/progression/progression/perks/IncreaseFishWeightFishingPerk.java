package me.mykindos.betterpvp.progression.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@Slf4j
public class IncreaseFishWeightFishingPerk implements Listener, ProgressionPerk {
    @Config(path = "fishing.perks.fish-weight.minLevel", defaultValue = "0")
    @Inject
    private int minLevel;

    @Config(path = "fishing.perks.fish-weight.maxLevel", defaultValue = "1000")
    @Inject
    private int maxLevel;

    @Config(path = "fishing.perks.fish-weight.increasePerLevel", defaultValue = "0.0005")
    @Inject
    private double increasePerLevel;

    @Inject
    private Progression progression;

    @Inject
    private Fishing fishing;


    @Override
    public String getName() {
        return "Increase Fish Weight Fishing";
    }

    @Override
    public Class<? extends ProgressionTree>[] acceptedTrees() {
        return new Class[]{
                Fishing.class
        };
    }

    @Override
    public boolean canUse(Player player, ProgressionData<?> data) {
        return minLevel <= data.getLevel();
    }

    @EventHandler (priority = EventPriority.NORMAL)
    public void onCatch(PlayerCaughtFishEvent event) {
        if ((event.getLoot() instanceof Fish fish)) {
            Player player = event.getPlayer();
            fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
                if (hasPerk) {
                    fishing.getLevel(player).whenComplete((level, throwable1) -> {
                        //cannot increase chance over the max level
                        if (level > maxLevel) level = maxLevel;
                        double levelIncrease = (int) (1 + (1000 - minLevel) * increasePerLevel);
                        int weight = (int) (fish.getWeight() * levelIncrease);
                        if (weight > fish.getType().getMaxWeight()) weight = fish.getType().getMaxWeight();
                        Bukkit.broadcast(UtilMessage.deserialize("Increasing weight from %s to %s", fish.getWeight(), weight));
                        //make a new fish
                        event.setLoot(new Fish(fish.getType(), weight));
                    }).exceptionally(throwable1 -> {
                        log.error("Failed to check if player " + event.getPlayer().getName() + " has a level ", throwable1);
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