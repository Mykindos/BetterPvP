package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageCause;
import me.mykindos.betterpvp.core.interaction.condition.InteractionCondition;
import me.mykindos.betterpvp.core.interaction.context.ExecutionKey;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.config.ConfigEntry;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.MultiRayTraceResult;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static me.mykindos.betterpvp.core.interaction.context.InputMeta.*;

@Getter
@Setter
public class BattoDoInteraction extends AbstractInteraction implements DisplayedInteraction {

    private static final ExecutionKey<Location> START_LOCATION = ExecutionKey.of("start_location");
    private static final ExecutionKey<Location> LAST_LOCATION = ExecutionKey.of("last_location");

    private final Config config;

    private double damage = 10.0;
    private double knockbackStrength = 2.0;
    private double dashStrength = 3.0;
    private double radius = 4.0;
    private double fireSeconds = 4.0;

    public BattoDoInteraction(Config config) {
        super("batto_do");
        this.config = config;
    }

    public void loadConfig() {
        this.damage = config.getConfig("damage", 10.0, Double.class);
        this.knockbackStrength = config.getConfig("knockback-strength", 2.0, Double.class);
        this.dashStrength = config.getConfig("dash-strength", 3.0, Double.class);
        this.radius = config.getConfig("radius", 4.0, Double.class);
        this.fireSeconds = config.getConfig("fire-seconds", 4.0, Double.class);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Batto-Do");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Dash forward and strike targets in your path, dealing damage, knocking them back, and setting them on fire.");
    }

    private void onSelect(@NotNull InteractionActor actor, @NotNull InteractionContext context, LivingEntity livingEntity) {
        UtilDamage.doDamage(new DamageEvent(
                livingEntity,
                actor.getEntity(),
                actor.getEntity(),
                new InteractionDamageCause(this).withCategory(DamageCauseCategory.MELEE),
                damage,
                "Batto-Do"
        ));

        UtilEntity.setFire(livingEntity, actor.getEntity(), (long) (fireSeconds * 1000));

        final Optional<Location> startLocation = context.get(START_LOCATION);
        if (startLocation.isPresent()) {
            final Vector direction = livingEntity.getLocation().subtract(startLocation.get()).toVector().normalize();
            UtilVelocity.velocity(livingEntity, actor.getEntity(), new VelocityData(
                    direction,
                    knockbackStrength,
                    true,
                    0.2,
                    0.1,
                    1.0,
                    true,
                    false
            ));
        }
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        final LivingEntity livingEntity = actor.getEntity();
        final Location center = livingEntity.getLocation();

        if (context.has(FIRST_RUN)) {
            final boolean success = UtilVelocity.velocity(livingEntity, null, new VelocityData(
                    livingEntity.getLocation().getDirection(),
                    dashStrength,
                    true,
                    0.01,
                    0,
                    0.01,
                    false,
                    true
            ));

            if (!success) {
                return new InteractionResult.Fail(InteractionResult.FailReason.CANCELLED);
            }

            // FX
            UtilServer.repeatTask(JavaPlugin.getPlugin(Champions.class), run -> {
                new SoundEffect("betterpvp", "item.hinokami_katana.swing", 1.2f, 1.2f).play(actor.getLocation());
                new SoundEffect(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 1f).play(center);
                new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 2f, 2).play(center);
                return true;
            }, 2, 2L);
            new SoundEffect(Sound.ITEM_TRIDENT_THROW, 1f, 2f).play(center);
            new SoundEffect(Sound.ITEM_TRIDENT_RIPTIDE_2, 0.3F, 0.8F).play(center);
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
            }, 7, 1L);

            context.set(START_LOCATION, center);
        } else {

            final Location lastLocation = context.get(LAST_LOCATION).orElseThrow();
            UtilEntity.interpolateMultiCollision(lastLocation,
                    center,
                    (float) radius,
                    (ent) -> UtilEntity.IS_ENEMY.test(livingEntity, ent))
                    .stream()
                    .flatMap(MultiRayTraceResult::stream)
                    .map(RayTraceResult::getHitEntity)
                    .filter(LivingEntity.class::isInstance)
                    .filter(ent -> !context.get(DAMAGED_ENTITIES).orElseThrow().contains(ent.getUniqueId()))
                    .map(LivingEntity.class::cast)
                    .forEach(ent -> {
                        this.onSelect(actor, context, ent);
                        context.get(DAMAGED_ENTITIES).orElseThrow().add(ent.getUniqueId());
                    });

        }

        context.set(LAST_LOCATION, center);
        return new InteractionResult.Running(600L, 1, true);
    }

    public static class PrepareInteraction extends CooldownInteraction implements DisplayedInteraction {

        private static final ExecutionKey<Long> WAIT_TIME = ExecutionKey.of("batto_do_start_time");
        private final NamespacedKey keySneak = new NamespacedKey("champions", "batto_do_sneak");
        private final NamespacedKey keyJump = new NamespacedKey("champions", "batto_do_jump");
        private final ConfigEntry<Double> cooldown;

        public PrepareInteraction(@NotNull CooldownManager cooldownManager, ConfigEntry<Double> cooldown) {
            super("batto_do_prepare", "Batto-Do", cooldownManager);
            this.cooldown = cooldown;
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.text("Batto-Do");
        }

        @Override
        public @NotNull Component getDisplayDescription() {
            return Component.text("Dash and strike targets in your path");
        }

        @Override
        public @NotNull List<InteractionCondition> getConditions() {
            return List.of();
        }

        @Override
        public double getCooldown() {
            return cooldown.get();
        }

        @Override
        protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
            if (!actor.isOnGround()) {
                return new InteractionResult.Running(1000L, 1, false);
            }

            final LivingEntity entity = actor.getEntity();

            final AttributeInstance sneak = Objects.requireNonNull(entity.getAttribute(Attribute.SNEAKING_SPEED));
            final AttributeModifier modifier = new AttributeModifier(keySneak, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            final AttributeInstance jump = Objects.requireNonNull(entity.getAttribute(Attribute.JUMP_STRENGTH));
            final AttributeModifier jumpModifier = new AttributeModifier(keyJump, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

            if (context.has(LAST_RUN)) {
                new SoundEffect(Sound.BLOCK_ANVIL_LAND, 2, 1).play(entity);
            } else if (!context.has(WAIT_TIME)) {
                new SoundEffect(Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.3f).play(actor.getLocation());
                new SoundEffect(Sound.ENTITY_BLAZE_SHOOT, 0.8F, 0.3F).play(actor.getLocation());
                new SoundEffect(Sound.ITEM_TRIDENT_RIPTIDE_3, 0.8F, 0.6F).play(actor.getLocation());

                final List<ParticleBuilder> particles = new ArrayList<>();
                for (Location location : UtilLocation.getCircumference(actor.getLocation(), 0.8, 15)) {
                    particles.add(Particle.FLAME.builder()
                            .count(1)
                            .extra(0)
                            .location(location)
                            .receivers(60));
                }
                for (Location location : UtilLocation.getCircumference(actor.getLocation(), 0.7, 15)) {
                    particles.add(Particle.TRIAL_SPAWNER_DETECTION.builder()
                            .count(1)
                            .extra(0.01)
                            .location(location)
                            .receivers(60));
                }

                UtilServer.repeatTask(JavaPlugin.getPlugin(Champions.class), run -> {
                    particles.forEach(ParticleBuilder::spawn);
                    return true;
                }, 4, 3L);

                if (sneak.getModifier(keySneak) == null) sneak.addTransientModifier(modifier);
                if (jump.getModifier(keyJump) == null) jump.addTransientModifier(jumpModifier);
                if (entity instanceof Player player) UtilPlayer.setWarningEffect(player, 1);

                final long startTime = context.get(InteractionContext.INTERACTION_START_TIME).orElseThrow();
                final long shiftTime = System.currentTimeMillis();
                final long elapsedTime = shiftTime - startTime;
                context.set(WAIT_TIME, elapsedTime + 500L);
            } else if (!actor.getEntity().isSneaking()) {
                return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
            }

            final Long delay = context.get(WAIT_TIME).orElseThrow();
            return new InteractionResult.Running(delay, 5, true);
        }

        @Override
        public void then(@NotNull InteractionActor actor, @NotNull InteractionContext context, @NotNull InteractionResult result, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
            super.then(actor, context, result, itemInstance, itemStack);
            final LivingEntity entity = actor.getEntity();
            if (entity instanceof Player player) UtilPlayer.clearWarningEffect(player);
            AttributeInstance sneak = Objects.requireNonNull(entity.getAttribute(Attribute.SNEAKING_SPEED));
            if (sneak.getModifier(keySneak) != null) sneak.removeModifier(keySneak);
            AttributeInstance jump = Objects.requireNonNull(entity.getAttribute(Attribute.JUMP_STRENGTH));
            if (jump.getModifier(keyJump) != null) jump.removeModifier(keyJump);
        }
    }
}
