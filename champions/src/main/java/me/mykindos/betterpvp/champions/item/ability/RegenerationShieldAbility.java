package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
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
public class RegenerationShieldAbility extends AbstractInteraction implements DisplayedInteraction {

    private double energyPerTick;
    private int regenerationAmplifier;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final EnergyService energyService;
    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;

    @Inject
    public RegenerationShieldAbility(Champions champions, EnergyService energyService, EffectManager effectManager) {
        super("shield");
        this.champions = champions;
        this.energyService = energyService;
        this.effectManager = effectManager;

        // Default values, will be overridden by config
        this.energyPerTick = 1.5;
        this.regenerationAmplifier = 5;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Shield");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Gain an amplified regeneration effect while using this ability.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        // Check energy
        if (!energyService.use(player, getName(), energyPerTick, true)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.ENERGY);
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
        effectManager.addEffect(player, player, EffectTypes.REGENERATION, getName(), regenerationAmplifier, 80L, true, false,
                (livingEntity) -> {
                    if (livingEntity instanceof Player p) {
                        return p.getInventory().getItemInMainHand().getType() != itemMaterial;
                    }
                    return false;
                });
    }
}
