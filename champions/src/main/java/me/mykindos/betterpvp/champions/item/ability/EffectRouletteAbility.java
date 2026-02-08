package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class EffectRouletteAbility extends CooldownInteraction implements DisplayedInteraction {

    @EqualsAndHashCode.Include
    private double duration;
    @EqualsAndHashCode.Include
    private double cooldown;

    private final EffectManager effectManager;

    @Inject
    public EffectRouletteAbility(EffectManager effectManager, CooldownManager cooldownManager) {
        super("effect_roulette", cooldownManager);
        this.effectManager = effectManager;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Effect Roulette");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Grants a random effect for a short duration. The effect can be a positive or negative one.");
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        LivingEntity entity = actor.getEntity();

        List<EffectType> effectTypesList = EffectTypes.getEffectTypes();
        List<EffectType> validEffectTypes = effectTypesList.stream()
                .filter(effect -> !effect.isSpecial())
                .toList();

        EffectType randomEffect = validEffectTypes.get(UtilMath.RANDOM.nextInt(validEffectTypes.size()));
        int randomLevel = UtilMath.RANDOM.nextInt(4) + 1;
        effectManager.addEffect(entity, randomEffect, randomLevel, (long) (duration * 1000));
        UtilMessage.message(entity, "Item",
                Component.text("You used ", NamedTextColor.GRAY)
                        .append(Component.text(getName(), NamedTextColor.YELLOW))
                        .append(Component.text(".", NamedTextColor.GRAY)));
        UtilMessage.message(entity, "Effect",
                Component.text("You have been granted ", NamedTextColor.GREEN)
                        .append(Component.text(randomEffect.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" Level " + randomLevel, NamedTextColor.GREEN))
                        .append(Component.text(" for " + duration + " seconds.", NamedTextColor.GREEN)));
        UtilSound.playSound(entity, Sound.ENTITY_PLAYER_BURP, 1f, 1f, false);
        return InteractionResult.Success.ADVANCE;
    }
}
