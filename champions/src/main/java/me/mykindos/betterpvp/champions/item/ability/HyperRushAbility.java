package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HyperRushAbility extends CooldownInteraction {

    private double cooldown;
    private int speedAmplifier;
    private int durationTicks;

    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;

    @Inject
    public HyperRushAbility(Champions champions, CooldownManager cooldownManager, EffectManager effectManager) {
        super("Hyper Rush",
                "Gain a burst of speed at a high level for a short duration.",
                cooldownManager);
        this.effectManager = effectManager;

        // Default values, will be overridden by config
        this.cooldown = 16.0;
        this.speedAmplifier = 3;
        this.durationTicks = 160; // 8 seconds (160 ticks)
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        LivingEntity entity = actor.getEntity();

        // Apply speed effect
        effectManager.addEffect(entity, EffectTypes.SPEED, speedAmplifier, (long) ((durationTicks / 20.0) * 1000));

        // Notify player and play sound
        UtilMessage.simpleMessage(entity, "Hyper Axe", "You used <green>Hyper Rush<gray>.");
        UtilSound.playSound(entity.getWorld(), entity.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
        return InteractionResult.Success.ADVANCE;
    }
}
