package me.mykindos.betterpvp.progression.profession.fishing.bait.ability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.jetbrains.annotations.NotNull;

/**
 * Ability for the Speedy Bait item.
 * Reduces the waiting time for fish to bite.
 */
@Singleton
@EqualsAndHashCode(callSuper = true)
public class SpeedyBaitAbility extends BaitAbility {

    /**
     * Creates a new speedy bait ability
     *
     * @param cooldownManager The cooldown manager
     */
    @Inject
    public SpeedyBaitAbility(CooldownManager cooldownManager) {
        super("speedy_bait", cooldownManager);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Speedy Bait");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Reduces the waiting time for fish to bite");
    }

    @Override
    protected Bait createBait() {
        return new Bait(getDuration()) {
            @Override
            public String getType() {
                return "Speedy";
            }

            @Override
            public Material getMaterial() {
                return Material.ORANGE_GLAZED_TERRACOTTA;
            }

            @Override
            public double getRadius() {
                return radius;
            }

            @Override
            protected void onTrack(FishHook hook) {
                hook.setWaitTime((int) (hook.getWaitTime() / multiplier));
            }
        };
    }
} 