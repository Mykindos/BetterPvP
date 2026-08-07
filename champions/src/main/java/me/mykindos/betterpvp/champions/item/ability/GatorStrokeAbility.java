package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.TemperInteraction;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.temper.TemperService;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import net.kyori.adventure.text.Component;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class GatorStrokeAbility extends TemperInteraction implements DisplayedInteraction {

    private double velocityStrength;
    private double skimmingDrainMultiplier;

    @EqualsAndHashCode.Exclude
    private final Champions champions;

    @Inject
    public GatorStrokeAbility(Champions champions, TemperService temperService) {
        super("gator_stroke", temperService);
        this.champions = champions;

        // Default values, will be overridden by config
        this.velocityStrength = 0.8;
        this.skimmingDrainMultiplier = 3.0;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("champions.ability.gator-stroke.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("champions.ability.gator-stroke.description");
    }

    /**
     * Skimming along the surface with your head out of the water costs the blade more than swimming
     * fully submerged does.
     */
    @Override
    protected double getDrainMultiplier(@NotNull InteractionActor actor, @NotNull InteractionContext context) {
        return UtilBlock.isWater(actor.getEntity().getEyeLocation().getBlock()) ? 1 : skimmingDrainMultiplier;
    }

    @Override
    protected @NotNull InteractionResult doTemperExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                          @NotNull ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (!UtilBlock.isInWater(player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), velocityStrength, false, 0, 0.11, 1.0, true);
        UtilVelocity.velocity(player, null, velocityData);
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.LAPIS_BLOCK);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, 0.8F, 1.5F);
        return InteractionResult.Success.ADVANCE;
    }
}
