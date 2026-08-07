package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.TemperInteraction;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.temper.TemperService;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ZephyrFlightAbility extends TemperInteraction implements DisplayedInteraction {

    private double flightSpeed;
    private double climbDrainMultiplier;

    @EqualsAndHashCode.Exclude
    private final Champions champions;

    @Inject
    public ZephyrFlightAbility(Champions champions, TemperService temperService) {
        super("zephyr_flight", temperService);
        this.champions = champions;

        // Default values, will be overridden by config
        this.flightSpeed = 0.7;
        this.climbDrainMultiplier = 2.0;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("champions.ability.zephyr-flight.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("champions.ability.zephyr-flight.description");
    }

    /**
     * Fighting gravity to gain height costs the blade more than riding the wind level or downwards.
     */
    @Override
    protected double getDrainMultiplier(@NotNull InteractionActor actor, @NotNull InteractionContext context) {
        return actor.getEntity().getLocation().getDirection().getY() > 0 ? climbDrainMultiplier : 1;
    }

    @Override
    protected @NotNull InteractionResult doTemperExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                         @NotNull ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (UtilBlock.isInLiquid(player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        // Steering is the look direction outright rather than an impulse on top of the existing
        // velocity, so held flight tracks the crosshair instead of drifting with whatever momentum
        // the last tick left behind.
        final Vector direction = player.getLocation().getDirection();
        final VelocityData velocityData = new VelocityData(direction, flightSpeed, false, 0, 0, flightSpeed, true);
        if (!UtilVelocity.velocity(player, null, velocityData)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (context.has(InputMeta.FIRST_RUN)) {
            new SoundEffect(Sound.ITEM_TRIDENT_RIPTIDE_2, 0.8F, 1.6F).play(player.getLocation());
        }

        new ParticleBuilder(Particle.CLOUD)
                .location(player.getLocation())
                .offset(0.2, 0.1, 0.2)
                .count(2)
                .extra(0)
                .receivers(60)
                .spawn();

        final float pitch = (float) (Math.random() * 0.5F + 0.3f);
        new SoundEffect(Sound.ENTITY_BREEZE_IDLE_AIR, pitch, 0.6F).play(player.getLocation());
        return InteractionResult.Success.ADVANCE;
    }
}
