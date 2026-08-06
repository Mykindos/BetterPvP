package me.mykindos.betterpvp.champions.item.corebrand;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageCause;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Fission is the Cascade movement option: a single hard shove in the direction you are looking that detonates
 * on whatever it reaches first.
 * <p>
 * Victims are thrown along the lunge reflected off the ground plane, so diving onto a group launches them
 * upward and the wielder has to turn and climb before the next one. Nothing is held down — the velocity is
 * applied once and released after a short window, in the manner of Vipersprint.
 */
@Singleton
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FissionLunge extends AbstractInteraction implements DisplayedInteraction {

    private double cost;
    private double velocity;
    private double duration;
    private double cooldown;
    private double slamRadius;
    private double slamDamage;
    private double impactVelocity;
    private double minimumLift;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final CascadeAbility cascadeAbility;
    @EqualsAndHashCode.Exclude
    private final CooldownManager cooldownManager;
    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;

    @Inject
    private FissionLunge(Champions champions, CascadeAbility cascadeAbility, CooldownManager cooldownManager,
                         EffectManager effectManager) {
        super("fission");
        this.champions = champions;
        this.cascadeAbility = cascadeAbility;
        this.cooldownManager = cooldownManager;
        this.effectManager = effectManager;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("champions.ability.fission.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("champions.ability.fission.description");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                   @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final CorebrandData data = cascadeAbility.getData(player);
        if (data == null || !data.isCascading()) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (cooldownManager.hasCooldown(player, "Fission")) {
            return new InteractionResult.Fail(InteractionResult.FailReason.COOLDOWN);
        }

        data.spend(cost);
        cooldownManager.use(player, "Fission", cooldown, false);
        lunge(player, itemStack);
        return InteractionResult.Success.ADVANCE;
    }

    private void lunge(Player player, ItemStack itemStack) {
        final Vector direction = player.getLocation().getDirection().normalize();
        UtilVelocity.velocity(player, null, new VelocityData(direction, velocity, false, 0.0D, 0.2D, 1.4D, true));

        effectManager.addEffect(player, player, EffectTypes.NO_FALL, "Fission", 9999, 3000, true, true, UtilBlock::isGrounded);

        new SoundEffect("emaginationfallendefender", "custom.spell.sfeproj_launch", 2F).play(player.getLocation());
        new SoundEffect(Sound.ENTITY_BEE_HURT, 0F).play(player.getLocation());

        if (itemStack != null) {
            UtilItem.damageItem(player, itemStack, 1);
        }

        final int maxTicks = (int) Math.max(1, duration * 20);
        new BukkitRunnable() {
            private int ticks;
            private Location lastLocation = player.getLocation();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                ticks++;
                playTrail(lastLocation, player.getLocation());

                if (ticks % 5 == 0) {
                    new SoundEffect(Sound.ENTITY_BEE_HURT, 0F).play(player.getLocation());
                }

                final LivingEntity struck = findCollision(player);
                if (struck != null) {
                    detonate(player, direction, struck.getLocation());
                    stop(player);
                    cancel();
                    return;
                }

                // A couple of ticks of grace so leaving the ground doesn't immediately read as landing on it
                if (ticks > 3 && UtilBlock.isGrounded(player)) {
                    detonate(player, direction, player.getLocation());
                    stop(player);
                    cancel();
                    return;
                }

                if (ticks >= maxTicks) {
                    stop(player);
                    cancel();
                    return;
                }

                lastLocation = player.getLocation();
            }
        }.runTaskTimer(champions, 1L, 1L);
    }

    /**
     * Every way the lunge can end — a body, the ground, or simply running out of window — closes on the same
     * burst, so the movement always reads as having landed rather than fizzling.
     */
    private void stop(Player player) {
        player.setVelocity(new Vector());

        Particle.EXPLOSION.builder()
                .location(UtilPlayer.getMidpoint(player))
                .count(1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    private LivingEntity findCollision(Player player) {
        final Vector travel = player.getVelocity();
        if (travel.lengthSquared() <= 0) {
            return null;
        }

        final Location midpoint = UtilPlayer.getMidpoint(player).clone();
        final Optional<LivingEntity> target = UtilEntity.interpolateCollision(midpoint,
                        midpoint.clone().add(travel.clone().normalize().multiply(0.6)),
                        1.2f,
                        ent -> UtilEntity.IS_ENEMY.test(player, ent))
                .map(RayTraceResult::getHitEntity)
                .map(LivingEntity.class::cast);
        return target.orElse(null);
    }

    /**
     * The blast. Everything nearby takes distance-scaled damage and is thrown along the reflected lunge.
     */
    private void detonate(Player player, Vector direction, Location epicentre) {
        final Vector knockback = CascadeAbility.reflect(direction, minimumLift);

        for (LivingEntity target : UtilEntity.getNearbyEnemies(player, epicentre, slamRadius)) {
            if (target.equals(player)) {
                continue;
            }

            final double falloff = Math.max(0, 1 - (UtilMath.offset(epicentre, target.getLocation()) / slamRadius));
            final double damage = slamDamage * (0.35 + (falloff * 0.65));

            UtilDamage.doDamage(new DamageEvent(target, player, player, new InteractionDamageCause(this), damage, "Fission"));
            UtilVelocity.velocity(target, player,
                    new VelocityData(knockback, impactVelocity * (0.6 + (falloff * 0.4)), false, 0.0D, 0.3D, 1.2D, true));
        }

        playBlast(epicentre, knockback);
    }

    private void playTrail(Location from, Location to) {
        for (Location point : VectorLine.withStepSize(from, to, 0.25).toLocations()) {
            Particle.DUST.builder()
                    .location(point)
                    .data(new Particle.DustOptions(Color.fromRGB(0x4F, 0xC3, 0xFF), 1.3F))
                    .count(4)
                    .offset(0.15, 0.15, 0.15)
                    .receivers(60)
                    .spawn();
        }
    }

    /**
     * Flame fired outwards along the blast direction, with cyan motes trailing it.
     */
    private void playBlast(Location epicentre, Vector direction) {
        final Location centre = epicentre.clone().add(0, 0.4, 0);
        new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE, 1.4F, 0.8F).play(centre);

        for (int point = 0; point < 24; point++) {
            final double angle = (Math.PI * 2 / 24) * point;
            final Vector outward = new Vector(Math.cos(angle), 0, Math.sin(angle)).add(direction).normalize();

            // count 0 with the offset as a vector and extra as speed fires the particle along that heading
            Particle.FLAME.builder()
                    .location(centre)
                    .count(0)
                    .offset(outward.getX(), outward.getY(), outward.getZ())
                    .extra(0.4)
                    .receivers(60)
                    .spawn();

            Particle.TRAIL.builder()
                    .location(centre.clone())
                    .data(new Particle.Trail(epicentre, Color.fromRGB(0x2E, 0xF2, 0xE0),  10))
                    .count(10)
                    .receivers(60)
                    .spawn();
        }
    }
}
