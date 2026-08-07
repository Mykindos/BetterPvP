package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.TemperInteraction;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.temper.TemperService;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class RegenerationShieldAbility extends TemperInteraction implements DisplayedInteraction {

    private int regenerationAmplifier;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;

    @Inject
    public RegenerationShieldAbility(Champions champions, TemperService temperService, EffectManager effectManager) {
        super("shield", temperService);
        this.champions = champions;
        this.effectManager = effectManager;

        // Default values, will be overridden by config
        this.regenerationAmplifier = 5;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("champions.ability.shield.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("champions.ability.shield.description");
    }

    @Override
    protected @NotNull InteractionResult doTemperExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                          @NotNull ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        // Apply regeneration effect with condition to remove when no longer holding item
        Material itemMaterial = itemStack != null ? itemStack.getType() : Material.AIR;
        applyRegeneration(player, itemMaterial);

        // Play particles and sound
        new SoundEffect(Sound.BLOCK_LAVA_POP, 1f, 2f).play(player.getLocation());
        new ParticleBuilder(Particle.HEART)
                .location(player.getEyeLocation().add(0, 0.25, 0))
                .offset(0.5, 0.5, 0.5)
                .extra(0.2f)
                .receivers(60)
                .spawn();
        return InteractionResult.Success.ADVANCE;
    }

    /**
     * Apply regeneration effect to player
     */
    private void applyRegeneration(Player player, Material itemMaterial) {
        effectManager.addEffect(player, player, EffectTypes.REGENERATION, "Shield", regenerationAmplifier, 80L, true, false,
                (livingEntity) -> {
                    if (livingEntity instanceof Player p) {
                        return p.getInventory().getItemInMainHand().getType() != itemMaterial;
                    }
                    return false;
                });
    }
}
