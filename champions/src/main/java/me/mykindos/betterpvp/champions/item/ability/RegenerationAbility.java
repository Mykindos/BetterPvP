package me.mykindos.betterpvp.champions.item.ability;

import me.mykindos.betterpvp.core.locale.Translations;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RegenerationAbility extends CooldownInteraction implements DisplayedInteraction {

    @EqualsAndHashCode.Include
    private double duration;
    @EqualsAndHashCode.Include
    private double cooldown;
    @EqualsAndHashCode.Include
    private int level;

    private final EffectManager effectManager;

    @Inject
    public RegenerationAbility(EffectManager effectManager, CooldownManager cooldownManager) {
        super("regeneration", cooldownManager);
        this.effectManager = effectManager;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("champions.ability.regeneration.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("champions.ability.regeneration.description");
    }

    @Override
    public double getCooldown(InteractionActor actor) {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        LivingEntity entity = actor.getEntity();

        effectManager.addEffect(entity, EffectTypes.REGENERATION, level, (long) (duration * 1000));
        UtilMessage.message(entity, "core.prefix.item", "champions.item.used", getDisplayName().applyFallbackStyle(NamedTextColor.YELLOW));
        UtilSound.playSound(entity, Sound.ENTITY_PLAYER_BURP, 1f, 1f, false);
        return InteractionResult.Success.ADVANCE;
    }
}
