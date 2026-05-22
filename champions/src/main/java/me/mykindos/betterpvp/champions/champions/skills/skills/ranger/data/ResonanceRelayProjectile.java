package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data;

import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ResonanceRelayProjectile extends Projectile {
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    private final ChampionsManager championsManager;
    private final @NotNull Location locationShotFrom;
    private final double buffDuration;
    private final int buffAmplifier;
    private final double damage;
    private final double maxDistance;
    private final @NotNull Skill skill;


    public ResonanceRelayProjectile(@NotNull Player caster, double hitboxSize, @NotNull Location location, long aliveTime,
                                    ChampionsManager championsManager, double buffDuration,
                                    int buffAmplifier, double damage, double maxDistance,
                                    @NotNull Skill skill) {
        super(caster, hitboxSize, location, aliveTime);
        this.championsManager = championsManager;
        this.locationShotFrom = location.clone();
        this.buffDuration = buffDuration;
        this.buffAmplifier = buffAmplifier;
        this.damage = damage;
        this.maxDistance = maxDistance;
        this.skill = skill;
    }

    @Override
    protected void onTick() {
        // Check max distance
        if (locationShotFrom.distance(location) > maxDistance) {
            this.markForRemoval = true;
        }

        // Play travel particles
        playParticles();

        location.getWorld().playSound(location, Sound.ENTITY_ALLAY_ITEM_GIVEN, 1.0f, 1.0f);
        location.getWorld().playSound(location, Sound.ITEM_SPEAR_LUNGE_1, 1.0f, 0.5f);
    }

    /**
     * Produces a circular particle effect along the path of the projectile.
     */
    private void playParticles() {
        final Collection<Player> receivers = location.getNearbyPlayers(60);

        Location[] line = interpolateLine();
        final double radius = hitboxSize;

        Vector prev = line[0].toVector();

        Vector direction = line[line.length - 1].toVector()
                .subtract(prev)
                .normalize();

        // pick stable up vector not parallel to direction
        Vector arbitrary = (Math.abs(direction.getY()) < 0.99)
                ? new Vector(0, 1, 0)
                : new Vector(1, 0, 0);

        Vector right = direction.clone().crossProduct(arbitrary).normalize();
        Vector up = right.clone().crossProduct(direction).normalize();

        final int step = 3;

        for (Location point : line) {
            Vector center = point.toVector();

            for (int i = 0; i < step; i++) {
                double angle = (Math.PI * 2 * i) / step;

                Vector offset = right.clone().multiply(Math.cos(angle) * radius)
                        .add(up.clone().multiply(Math.sin(angle) * radius));

                Particle.DUST.builder()
                        .location(center.clone().add(offset).toLocation(location.getWorld()))
                        .count(1)
                        .color(Color.fromRGB(0, 255, 120))
                        .receivers(receivers)
                        .spawn();
            }
        }
    }

    @Override
    protected boolean canCollideWith(Entity entity) {
        if (entity instanceof ArmorStand) return false;
        if (entity.equals(caster)) return false;
        if (!(entity instanceof LivingEntity livingTarget)) return false;
        if (hitEntities.contains(livingTarget)) return false;

        // Not sure how expensive isEntityFriendly is; if needed, hitEntities could become a map and cache the result.
        if (UtilEntity.isEntityFriendly(caster, livingTarget)) return true;

        final EntityCanHurtEntityEvent event = new EntityCanHurtEntityEvent(caster, livingTarget);
        event.callEvent();

        return event.getResult() != Event.Result.DENY;
    }

    @Override
    protected CollisionResult onCollide(RayTraceResult result) {
        final @Nullable Entity hitEntity = result.getHitEntity();
        if (hitEntity == null) {
            this.markForRemoval = true;
            return CollisionResult.IMPACT;  // either we hit a block or exceeded max distance
        }

        final @NotNull LivingEntity target = (LivingEntity) hitEntity;
        hitEntities.add(target);

        caster.playSound(caster.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1f);

        // Do something to target based on relation to caster
        if (UtilEntity.isEntityFriendly(caster, target)) {
            doBuffForAlly(target);
        } else {
            doDamageToEnemy(target);
        }

        final @NotNull String classTypeName = skill.getClassType().getName();

        UtilMessage.simpleMessage(caster, classTypeName,
                "You hit <alt>" + target.getName() + "</alt> with <alt>" + skill.getName());

        UtilMessage.simpleMessage(target, classTypeName,
                "<alt>" + caster.getName() + "</alt> hit you with <alt>" + skill.getName());


        return CollisionResult.CONTINUE;  // Ray projectile; always continues until block
    }

    private void doBuffForAlly(LivingEntity ally) {
        championsManager.getEffects().addEffect(ally,
                caster,
                EffectTypes.STRENGTH,
                skill.getName(),
                buffAmplifier,
                (long) (buffDuration * 1000L));
    }

    private void doDamageToEnemy(LivingEntity enemy) {
        UtilDamage.doDamage(new DamageEvent(
                enemy, caster, null, new SkillDamageCause(skill), damage, skill.getName())
        );
    }
}
