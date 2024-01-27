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
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

@BPvPListener
@Singleton
@Slf4j
public class IncreaseFishWeightFishingPerk implements Listener, ProgressionPerk, ChanceHandler {
    @Config(path = "fishing.perks.fish-weight.enabled", defaultValue = "true")
    @Inject
    private boolean enabled;

    @Config(path = "fishing.perks.fish-weight.minLevel", defaultValue = "0")
    @Inject
    private int minLevel;

    @Config(path = "fishing.perks.fish-weight.maxLevel", defaultValue = "1000")
    @Inject
    private int maxLevel;

    @Config(path = "fishing.perks.fish-weight.increasePerLevel", defaultValue = "0.001")
    @Inject
    private double increasePerLevel;

    @Config(path = "fishing.perks.fish-weight.increaseWeight", defaultValue = "0.20")
    @Inject
    private double increaseWeight;

    @Inject
    private Progression progression;

    @Inject
    private Fishing fishing;


    @Override
    public String getName() {
        return "Increase Fish Weight";
    }

    @Override
    public List<String> getDescription(Player player, ProgressionData<?> data) {
        List<String> description = new ArrayList<>(List.of(
                "Increase the amount of items dropped while fishing by",
                "<stat>" + increasePerLevel + "%</stat> per Mining Level.",
                "Every <stat>100%</stat>, you have a guaranteed drop, with the remainder being",
                "the chance to get another drop."
        ));
        if (canUse(player, data)) {
            description.add("Currently increases your chances by <val>" + data.getLevel() * increasePerLevel + "%</val>");
        }
        return description;
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

    public double getIncrease(int level) {
        if (level > maxLevel) level = maxLevel;
        //make leveling more intuitive
        level = level - minLevel;
        return level * increasePerLevel;
    }

    @EventHandler (priority = EventPriority.NORMAL)
    public void onCatch(PlayerCaughtFishEvent event) {
        if (!enabled) return;
        if(!fishing.isEnabled()) return;
        if ((event.getLoot() instanceof Fish fish)) {
            Player player = event.getPlayer();
            fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
                if (hasPerk) {
                    fishing.getLevel(player).whenComplete((level, throwable1) -> {
                        double chanceMultiplier = getChance(getIncrease(level));
                        if (chanceMultiplier == 0) {
                            return;
                        }
                        int weight = (int) (fish.getWeight() + (fish.getWeight() * chanceMultiplier * increaseWeight));
                        //make a new fish if weight is > than old weight
                        if (weight >= fish.getWeight()) {
                            event.setLoot(new Fish(fish.getType(), weight));
                        }
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