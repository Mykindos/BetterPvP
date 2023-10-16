package me.mykindos.betterpvp.progression.progression.perks;


<<<<<<< HEAD
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
=======
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
>>>>>>> 54b7ddd5 (Basic start for a fishing start multiplier)
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
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.PlayerInventory;

@BPvPListener
@Singleton
@Slf4j
public class DropMultiplierFishingPerk implements Listener, ProgressionPerk, DropMultiplier {

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
        if (event.getReason() != PlayerStopFishingEvent.FishingResult.CATCH) return;
        if (event.getLoot() instanceof Fish loot) {
            Player player = event.getPlayer();
            fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
                if (hasPerk) {
                    fishing.getLevel(player).whenComplete((level, throwable1) -> {
                        //cannot increase chance over the max level
                        if (level > maxLevel) level = maxLevel;
                        int extraDrops = getExtraDrops(level * increasePerLevel);
                        Location playerLocation = player.getLocation();
                        for (int i = 0; i < extraDrops; i++) {
                            playerLocation.getWorld().dropItemNaturally(playerLocation, loot.getFishBucket());
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