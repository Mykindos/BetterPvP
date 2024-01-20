package me.mykindos.betterpvp.progression.progression.perks.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerStartFishingEvent;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerStopFishingEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@BPvPListener
@Singleton
public class DamageReductionFishingPerk implements Listener, ProgressionPerk {
    @Config(path = "fishing.perks.damage-reduction.enabled", defaultValue = "true")
    @Inject
    private boolean enabled;

    @Config(path = "fishing.perks.damage-reduction.minLevel", defaultValue = "0")
    @Inject
    private int minLevel;

    @Config(path = "fishing.perks.damage-reduction.maxLevel", defaultValue = "1000")
    @Inject
    private int maxLevel;

    @Config(path = "fishing.perks.damage-reduction.decreasePerLevel", defaultValue = "0.001")
    @Inject
    private double decreasePerLevel;

    @Inject
    private Progression progression;

    @Inject
    private Fishing fishing;

    private Set<UUID> active = new HashSet<>();


    @Override
    public String getName() {
        return "Damage Reduction Fishing";
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



    @EventHandler
    public void onStartFishing(PlayerStartFishingEvent event) {
        if (!enabled) return;
        if (!fishing.isEnabled()) return;
        Player player = event.getPlayer();
        fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
            if (hasPerk) {
                active.add(player.getUniqueId());
            }
        }).exceptionally(throwable -> {
            log.error("Failed to check if player " + event.getPlayer().getName() + " has perk " + getName(), throwable);
            return null;
        });
    }

    @EventHandler
    public void onStopFishing(PlayerStopFishingEvent event) {
        if (!enabled) return;
        if (!fishing.isEnabled()) return;
        active.removeIf(o -> o == event.getPlayer().getUniqueId());
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onDamage (CustomDamageEvent event) {
        if (!enabled) return;
        if (!fishing.isEnabled()) return;
        if (event.getDamagee() instanceof Player player) {
            if (active.contains(player.getUniqueId())) {
                fishing.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
                    if (hasPerk) {
                        fishing.getLevel(player).whenComplete((level, throwable1) -> {
                            //cannot increase chance over the max level
                            if (level > maxLevel) level = maxLevel;
                            double damageReduction = Math.max(1 - ((level - minLevel) * decreasePerLevel), 0);
                            event.setDamage(event.getDamage() * damageReduction);
                        }).exceptionally(throwable1 -> {
                            log.error("Failed to check if player " + player.getName() + " has a level ", throwable);
                            return null;
                        });
                    }
                }).exceptionally(throwable -> {
                    log.error("Failed to check if player " + player.getName() + " has perk " + getName(), throwable);
                    return null;
                });
            }
        }
    }
}
