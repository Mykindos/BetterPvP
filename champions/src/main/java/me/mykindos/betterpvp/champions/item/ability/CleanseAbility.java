package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class CleanseAbility extends CooldownInteraction implements DisplayedInteraction {

    @EqualsAndHashCode.Include
    private double duration;
    @EqualsAndHashCode.Include
    private double cooldown;

    private final EffectManager effectManager;

    @Inject
    public CleanseAbility(EffectManager effectManager, CooldownManager cooldownManager) {
        super("cleanse", cooldownManager);
        this.effectManager = effectManager;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Cleanse");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Cleanses negative effects and grants immunity for a short duration.");
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        LivingEntity entity = actor.getEntity();
        if (entity instanceof Player player) {
            if (UtilServer.callEvent(new EffectClearEvent(player)).isCancelled()) {
                return new InteractionResult.Fail(InteractionResult.FailReason.CANCELLED);
            }
        }

        UtilMessage.message(entity, "Item",
                Component.text("You used ", NamedTextColor.GRAY)
                        .append(Component.text(getName(), NamedTextColor.YELLOW))
                        .append(Component.text(".", NamedTextColor.GRAY)));
        UtilSound.playSound(entity, Sound.ENTITY_GENERIC_DRINK, 1f, 1f, false);
        UtilSound.playSound(entity.getWorld(), entity.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 1.2f);
        effectManager.addEffect(entity, EffectTypes.IMMUNE, (long) (duration * 1000L));
        entity.setFireTicks(0);
        return InteractionResult.Success.ADVANCE;
    }
}
