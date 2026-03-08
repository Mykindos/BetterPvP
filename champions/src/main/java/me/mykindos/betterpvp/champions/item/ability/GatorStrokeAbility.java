package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import net.kyori.adventure.text.Component;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class GatorStrokeAbility extends AbstractInteraction implements DisplayedInteraction {

    private double velocityStrength;
    private double energyPerTick;
    private double skimmingEnergyMultiplier;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final EnergyService energyService;

    @Inject
    public GatorStrokeAbility(Champions champions, EnergyService energyService) {
        super("gator_stroke");
        this.champions = champions;
        this.energyService = energyService;

        // Default values, will be overridden by config
        this.velocityStrength = 0.7;
        this.energyPerTick = 1.0;
        this.skimmingEnergyMultiplier = 3.0;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Gator Stroke");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Propels the user at high speed. This ability only works in water.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (!UtilBlock.isInWater(player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        double energyToUse = energyPerTick;
        if (!UtilBlock.isWater(player.getEyeLocation().getBlock().getRelative(BlockFace.DOWN))) {
            energyToUse *= skimmingEnergyMultiplier;
        }

        if (!energyService.use(player, "Gator Stroke", energyToUse, true)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.ENERGY);
        }

        VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), velocityStrength, false, 0, 0.11, 1.0, true);
        UtilVelocity.velocity(player, null, velocityData);
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.LAPIS_BLOCK);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, 0.8F, 1.5F);
        return InteractionResult.Success.ADVANCE;
    }
}
