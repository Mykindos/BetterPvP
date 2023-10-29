package me.mykindos.betterpvp.progression.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.tree.fishing.loot.TreasureType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@Slf4j
public class IncreaseTreasureChanceFishingPerk implements Listener, ProgressionPerk {
    @Config(path = "fishing.perks.legend-chance.minLevel", defaultValue = "750")
    @Inject
    private int minLevel;

    @Config(path = "fishing.perks.legend-chance.maxLevel", defaultValue = "1000")
    @Inject
    private int maxLevel;

    @Config(path = "fishing.perks.legend-chance.increasePerLevel", defaultValue = "0.25")
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

    @EventHandler (priority = EventPriority.LOW)
    public void onCatch(PlayerCaughtFishEvent event) {
        //if we are already fishing up a treasure, don't change anything
        if ((event.getLoot().getType() instanceof TreasureType)) return;
        Player player = event.getPlayer();
        fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
            if (hasPerk) {
                fishing.getLevel(player).whenComplete((level, throwable1) -> {
                    //cannot increase chance over the max level
                    if (level > maxLevel) level = maxLevel;
                    double levelIncrease = (int) (1 + (1000 - minLevel) * increasePerLevel);
                    WeighedList<FishingLootType> lootTypes = new WeighedList<>();
                    //regenerate the WeighedList, increasing the frequency of TreasureTypes
                    for (FishingLootType type : fishing.getLootTypes()) {
                        if (type instanceof TreasureType treasureType) {
                            int frequency = (int) (treasureType.getFrequency() * levelIncrease);
                            lootTypes.add(frequency, 1, type);
                            Bukkit.broadcast(UtilMessage.deserialize("Changed frequency of %s from %s to %s", treasureType.getName(), treasureType.getFrequency(), frequency));
                        } else {
                            lootTypes.add(type.getFrequency(), 1, type);
                        }
                    }
                    //reroll with increased treasure chances
                    FishingLootType potentialType = lootTypes.random();
                    //if the new type is a treasure, keep it.
                    if (potentialType instanceof TreasureType) {
                        event.setLoot(potentialType.generateLoot());
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