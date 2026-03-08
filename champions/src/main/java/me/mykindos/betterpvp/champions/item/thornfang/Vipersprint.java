package me.mykindos.betterpvp.champions.item.thornfang;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageCause;
import me.mykindos.betterpvp.core.interaction.context.ExecutionKey;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.MultiRayTraceResult;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static me.mykindos.betterpvp.core.interaction.context.InputMeta.DAMAGED_ENTITIES;
import static me.mykindos.betterpvp.core.interaction.context.InputMeta.LAST_RUN;
import static me.mykindos.betterpvp.core.interaction.context.InteractionContext.INTERACTION_START_TIME;

/**
 * Vipersprint - A duration-based dash ability that uses the Running interaction result.
 * The InteractionListener handles ticking this ability until it completes.
 */
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class Vipersprint extends AbstractInteraction implements Listener, DisplayedInteraction {

    // Context keys for storing state across ticks
    private static final ExecutionKey<Location> LAST_LOCATION = ExecutionKey.of("vipersprint_last_loc");

    private final CooldownManager cooldownManager;
    private final ClientManager clientManager;
    private final EffectManager effectManager;
    private final NamespacedKey key;

    private double cooldown;
    private double duration;
    private double speed;
    private double damage;
    private double poisonSeconds;
    private int poisonAmplifier;

    public Vipersprint(Champions champions, CooldownManager cooldownManager, ClientManager clientManager, EffectManager effectManager) {
        super("vipersprint");
        this.key = new NamespacedKey(champions, "vipersprint");
        this.cooldownManager = cooldownManager;
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        Bukkit.getPluginManager().registerEvents(this, champions);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Vipersprint");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Dash forward at high speed, curving your path with your aim while cutting through anything in your way. Hitting an enemy resets your cooldown.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                   @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        final LivingEntity livingEntity = actor.getEntity();
        // Check cooldown
        if (livingEntity instanceof Player player && cooldownManager.hasCooldown(player, "Vipersprint")) {
            return new InteractionResult.Fail(InteractionResult.FailReason.COOLDOWN);
        }

        // Check expiry
        if (context.has(LAST_RUN) ||
                UtilTime.elapsed(context.get(INTERACTION_START_TIME).orElseThrow(), (long) (duration * 1000L))) {
            stop(livingEntity);
            return InteractionResult.Success.ADVANCE;
        }

        // Continue dashing
        applyStepHeight(livingEntity);
        Location lastLocation = context.get(LAST_LOCATION).orElse(livingEntity.getLocation());
        Set<UUID> damagedEntities = context.get(DAMAGED_ENTITIES).orElse(new HashSet<>());

        dash(livingEntity, lastLocation);
        poisonNearby(livingEntity, lastLocation, damagedEntities);

        // Update state
        context.set(LAST_LOCATION, livingEntity.getLocation());
        context.set(DAMAGED_ENTITIES, damagedEntities);

        return InteractionResult.Success.ADVANCE;
    }

    private void poisonNearby(LivingEntity livingEntity, Location lastLocation, Set<UUID> damagedEntities) {
        List<LivingEntity> enemies = UtilEntity.interpolateMultiCollision(
                        lastLocation,
                        livingEntity.getLocation(),
                        0.9f,
                        ent -> UtilEntity.IS_ENEMY.test(livingEntity, ent))
                .stream()
                .flatMap(MultiRayTraceResult::stream)
                .map(RayTraceResult::getHitEntity)
                .filter(LivingEntity.class::isInstance)
                .filter(ent -> !damagedEntities.contains(ent.getUniqueId()))
                .map(LivingEntity.class::cast)
                .toList();

        for (LivingEntity enemy : enemies) {
            if (damagedEntities.contains(enemy.getUniqueId())) {
                continue;
            }

            DamageEvent event = UtilDamage.doDamage(new DamageEvent(
                    enemy,
                    livingEntity,
                    livingEntity,
                    new InteractionDamageCause(this).withBukkitCause(EntityDamageEvent.DamageCause.POISON),
                    damage,
                    "Vipersprint"
            ));

            if (!event.isCancelled()) {
                effectManager.addEffect(enemy, livingEntity, EffectTypes.POISON, "Vipersprint", poisonAmplifier, (long) (poisonSeconds * 1000L));

                VelocityData data = new VelocityData(livingEntity.getLocation().getDirection(),
                        1.2, false, 0, 0.2, 1.0, true);
                UtilVelocity.velocity(enemy, livingEntity, data);
                damagedEntities.add(enemy.getUniqueId());
            }
        }
    }

    private void applyStepHeight(LivingEntity livingEntity) {
        AttributeInstance attribute = livingEntity.getAttribute(Attribute.STEP_HEIGHT);
        if (attribute != null && attribute.getModifier(key) == null) {
            attribute.addTransientModifier(new AttributeModifier(key, 0.4, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private void dash(LivingEntity livingEntity, Location lastLocation) {
        // Apply velocity
        VelocityData velocityData = new VelocityData(livingEntity.getLocation().getDirection(), speed, false, 0, 0, 1.0, true);
        UtilVelocity.velocity(livingEntity, null, velocityData);

        // Particle cues
        List<Location> points = new ArrayList<>();
        if (lastLocation != null) {
            Location[] calc = VectorLine.withStepSize(lastLocation, livingEntity.getLocation(), 0.2).toLocations();
            points.addAll(List.of(calc));
        } else {
            points.add(livingEntity.getLocation());
        }

        for (Location point : points) {
            Particle.DUST_PILLAR.builder()
                    .data(Material.VINE.createBlockData())
                    .count(10)
                    .extra(0.01)
                    .location(point)
                    .receivers(60)
                    .spawn();
        }

        new SoundEffect(Sound.BLOCK_WET_GRASS_BREAK, 0.8f, 1f).play(livingEntity.getLocation());
    }

    private void stop(LivingEntity entity) {
        // Remove step height modifier
        AttributeInstance attribute = entity.getAttribute(Attribute.STEP_HEIGHT);
        if (attribute != null) {
            attribute.removeModifier(key);
        }

        if (entity instanceof Player player) {
            cooldownManager.use(player, "Vipersprint", cooldown, true);
        }

        // Stop velocity
        entity.setVelocity(new Vector());

        new SoundEffect(Sound.BLOCK_GRASS_PLACE, 0.8f, 1f).play(entity.getLocation());
        new SoundEffect(Sound.BLOCK_FIRE_EXTINGUISH, 1.8f, 0.2f).play(entity.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMeleeAttack(DamageEvent event) {
        if (event.getDamager() instanceof Player player) {
            cooldownManager.removeCooldown(player, "Vipersprint", false);
        }
    }
}
