package me.mykindos.betterpvp.clans.clans.fatigue.punishment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.fatigue.FatigueTier;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import org.bukkit.entity.Player;

/**
 * Lingering heavy-limbs slowness once the player is back on their feet. The
 * level and duration scale with how exhausted they are, so a Worn player barely
 * notices while an Exhausted one is visibly hobbled for a while.
 */
@Singleton
public class SlownessPunishment implements FatiguePunishment {

    private final EffectManager effectManager;

    @Inject
    @Config(path = "clans.fatigue.slowness.secondsPerTier", defaultValue = "0.0")
    private double secondsPerTier;

    @Inject
    public SlownessPunishment(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @Override
    public boolean appliesTo(FatigueTier tier) {
        return tier.requiresHold();
    }

    @Override
    public void apply(Player player, FatigueTier tier) {
        // WORN -> I, WEARY -> II, EXHAUSTED -> III
        final int level = Math.max(1, tier.ordinal());
        final long durationMillis = (long) (secondsPerTier * tier.ordinal() * 1000L);
        effectManager.addEffect(player, EffectTypes.SLOWNESS, level, durationMillis);
    }
}
