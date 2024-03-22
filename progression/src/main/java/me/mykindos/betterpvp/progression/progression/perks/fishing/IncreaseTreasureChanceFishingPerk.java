package me.mykindos.betterpvp.progression.progression.perks.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.tree.fishing.loot.SwimmerType;
import me.mykindos.betterpvp.progression.tree.fishing.loot.TreasureType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@CustomLog
public class IncreaseTreasureChanceFishingPerk implements Listener, ProgressionPerk {
    @Config(path = "fishing.perks.treasure-chance.enabled", defaultValue = "true")
    @Inject
    private boolean enabled;

    @Config(path = "fishing.perks.treasure-chance.minLevel", defaultValue = "750")
    @Inject
    private int minLevel;

    @Config(path = "fishing.perks.treasure-chance.maxLevel", defaultValue = "1000")
    @Inject
    private int maxLevel;

    @Config(path = "fishing.perks.treasure-chance.increasePerLevel", defaultValue = "0.25")
    @Inject
    private double increasePerLevel;

    @Inject
    private Progression progression;

    @Inject
    private Fishing fishing;


    @Override
    public String getName() {
        return "Increase Treasure Chance Fishing";
    }

    @Override
    public Class<? extends ProgressionTree>[] acceptedTrees() {
        return new Class[]{
                Fishing.class
        };
    }

    @Override
    public boolean canUse(Player player, ProgressionData<?> data) {
        return minLevel <= data.getLevel() &&
                fishing.getLootTypes().getElements().stream().anyMatch(o -> (o instanceof TreasureType));
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onCatch(PlayerCaughtFishEvent event) {
        if (!enabled) return;
        if(!fishing.isEnabled()) return;
        // Only reroll for swimmers
        if (!(event.getLoot().getType() instanceof SwimmerType)) return;

        Player player = event.getPlayer();
        fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
            if (hasPerk) {
                fishing.getLevel(player).whenComplete((level, throwable1) -> {
                    //cannot increase chance over the max level
                    if (level > maxLevel) level = maxLevel;
                    //make leveling more intuitive
                    level = level - minLevel;
                    double levelIncrease = level * increasePerLevel; // 100-based
                    if (Math.random() * 100 > levelIncrease) return;

                    WeighedList<FishingLootType> lootTypes = new WeighedList<>();
                    // Populate the WeighedList with only TreasureTypes
                    for (FishingLootType type : fishing.getLootTypes()) {
                        if (type instanceof TreasureType) {
                            lootTypes.add(type.getFrequency(), 1, type);
                        }
                    }

                    FishingLootType potentialType = lootTypes.random();
                    event.setLoot(potentialType.generateLoot());
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