package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageCause;
import me.mykindos.betterpvp.core.interaction.context.ChainKey;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntityFilters;
import me.mykindos.betterpvp.core.utilities.model.selector.origin.EntityOrigin;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.ShapeEntitySelector;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.interaction.context.InputMeta.FIRST_RUN;
import static me.mykindos.betterpvp.core.interaction.context.InteractionContext.INTERACTION_START_TIME;

@Getter
@Setter
public class BladeSmashInteraction extends SelectorInteraction {

    private static final ChainKey<Boolean> SLAMMED = ChainKey.of("SLAMMED");

    private double jumpStrength;
    private double slamStrength;
    private double radius;
    private double damage;
    private double fireSeconds;

    public BladeSmashInteraction() {
        super(null);
        this.setSelector(entity -> ShapeEntitySelector.cylinder(new EntityOrigin(entity, false), radius, 0.4)
                .withFilter(EntityFilters.combatEnemies()));
    }

    @Override
    protected void onSelect(@NotNull InteractionActor actor, @NotNull InteractionContext context, LivingEntity livingEntity) {
        UtilDamage.doDamage(new DamageEvent(
                livingEntity,
                actor.getEntity(),
                actor.getEntity(),
                new InteractionDamageCause(this).withCategory(DamageCauseCategory.MELEE),
                damage,
                "Blade Smash"
        ));

        UtilEntity.setFire(livingEntity, actor.getEntity(), (long) (fireSeconds * 1000));
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        final Location center = actor.getEntity().getLocation();

        if (context.has(FIRST_RUN)) {
            final boolean success = UtilVelocity.velocity(actor.getEntity(), null, new VelocityData(
                    new Vector(0, 1, 0),
                    jumpStrength,
                    0,
                    1,
                    false
            ));

            if (!success) {
                return new InteractionResult.Fail(InteractionResult.FailReason.CANCELLED);
            }

            // Sound
            UtilServer.repeatTask(JavaPlugin.getPlugin(Champions.class), run -> {
                new SoundEffect(Sound.ENTITY_ENDER_DRAGON_FLAP, 2f, 1f).play(center);
                return true;
            }, 2, 2L);

            return new InteractionResult.Running(1000L, 2, false);
        } else if (!context.has(SLAMMED)) {
            if (UtilTime.elapsed(context.get(INTERACTION_START_TIME).orElseThrow(), 500)) {
                // Slam down
                final boolean success = UtilVelocity.velocity(actor.getEntity(), null, new VelocityData(
                        new Vector(0, -1, 0),
                        slamStrength,
                        0,
                        -1,
                        false
                ));

                if (!success) {
                    return new InteractionResult.Fail(InteractionResult.FailReason.CANCELLED);
                }

                context.set(SLAMMED, true);
            }
            return new InteractionResult.Running(1000L, 1, false);
        } else if (!UtilBlock.isGrounded(actor.getEntity())) {
            // waiting for them to land
            return new InteractionResult.Running(1000L, 1, false);
        }

        // Play redstone rings
        for (double r = 0.5; r <= radius; r += 0.5) {
            final int points = (int) (Math.PI * r * 10);
            final List<Location> ringLocations = UtilLocation.getCircumference(center, r, points);
            final List<ParticleBuilder> redstoneParticles = new ArrayList<>();
            for (Location location : ringLocations) {
                final ParticleBuilder particles = Particle.DUST.builder()
                        .count(1)
                        .color(Color.fromRGB(255, 176, 41))
                        .location(location)
                        .receivers(60);
                redstoneParticles.add(particles);
            }

            UtilServer.repeatTask(JavaPlugin.getPlugin(Champions.class), run -> {
                redstoneParticles.forEach(ParticleBuilder::spawn);
                return true;
            }, 2, 10L);
        }

        UtilServer.repeatTask(JavaPlugin.getPlugin(Champions.class), run -> {
            Particle.LAVA.builder()
                    .count(5)
                    .offset(radius / 2, 0, radius / 2)
                    .location(center)
                    .receivers(60)
                    .spawn();
            Particle.FLAME.builder()
                    .count(10)
                    .extra(0.2)
                    .offset(radius / 2, 0.5, radius / 2)
                    .location(center)
                    .receivers(60)
                    .spawn();
            return true;
        }, 2, 4L);

        new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.1f, 1.2f).play(center);
        new SoundEffect(Sound.ITEM_TRIDENT_RIPTIDE_2, 0.7f, 0.5f).play(center);
        new SoundEffect(Sound.ENTITY_BLAZE_SHOOT, 1.7f, 0.9f).play(center);
        new SoundEffect(Sound.ENTITY_BLAZE_SHOOT, 1.3f, 0.9f).play(center);

        super.doExecute(actor, context, itemInstance, itemStack);
        return actor.getEntity().isSneaking()
                ? InteractionResult.Success.ADVANCE
                : InteractionResult.Success.NO_ADVANCE;
    }
}
