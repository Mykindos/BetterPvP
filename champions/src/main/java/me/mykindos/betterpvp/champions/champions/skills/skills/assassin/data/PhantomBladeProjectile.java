package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE;

/**
 * A thrown blade that passes through every enemy in its path before embedding itself where it stops.
 * It hangs at that position for a limited time, during which the caster can recall to it.
 */
public class PhantomBladeProjectile extends Projectile {

    private final ItemDisplay display;
    private final double damage;
    private final Skill skill;
    private final long anchorTime;
    private final Set<UUID> hitEntities = new HashSet<>();

    public PhantomBladeProjectile(Player caster, double hitboxSize, Location location, long flightTime, long anchorTime,
                                  ItemStack blade, double damage, Skill skill) {
        super(caster, hitboxSize, location, flightTime);
        this.damage = damage;
        this.skill = skill;
        this.anchorTime = anchorTime;
        this.display = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(blade);
            spawned.setPersistent(false);

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set(1.5, 1.5, 1.5);
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(90));
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(45));
            spawned.setTransformation(transformation);

            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
        });
    }

    /**
     * @return the location the blade is embedded at, which the caster can recall to
     */
    public Location getAnchorLocation() {
        return display.getLocation();
    }

    /**
     * @return true once the blade has hung in place for its full duration and should be cleaned up
     */
    public boolean hasFaded() {
        return impacted && UtilTime.elapsed(impactTime, anchorTime);
    }

    @Override
    protected void onTick() {
        if (impacted) {
            spawnBladeDust(display.getLocation().add(0, 0.25, 0), 1.0f);
            return;
        }

        for (Location point : interpolateLine()) {
            spawnBladeDust(point, 1.3f);
        }

        display.teleport(location.clone().setDirection(velocity));
    }

    @Override
    protected CollisionResult onCollide(RayTraceResult result) {
        final Entity hit = result.getHitEntity();
        if (hit == null) {
            return CollisionResult.IMPACT;
        }

        if (hit instanceof LivingEntity target && hitEntities.add(target.getUniqueId())) {
            pierce(target);
        }

        return CollisionResult.CONTINUE;
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        // Pull the blade back out of whatever it embedded itself in so it stays visible
        if (velocity.lengthSquared() > 0) {
            this.location = location.clone().subtract(velocity.clone().normalize().multiply(0.3));
        }

        display.teleport(this.location);
        redirect(null);

        new SoundEffect(Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.4f, 0.7f).play(this.location);
        new SoundEffect(Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.6f, 0.5f).play(this.location);
    }

    private void pierce(LivingEntity target) {
        UtilDamage.doDamage(new DamageEvent(target,
                caster,
                null,
                new SkillDamageCause(skill).withBukkitCause(PROJECTILE),
                damage,
                skill.getName()));

        new SoundEffect(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.8f, 0.6f).play(target.getLocation());

        UtilMessage.message(target, skill.getClassType().getDisplayName(), "champions.skill.hit-by",
                Component.text(caster.getName(), NamedTextColor.YELLOW), skill.getDisplayName().color(NamedTextColor.GREEN));
        UtilMessage.message(caster, skill.getClassType().getDisplayName(), "champions.skill.hit-target",
                Component.text(target.getName(), NamedTextColor.YELLOW), skill.getDisplayName().color(NamedTextColor.GREEN));
    }

    private void spawnBladeDust(Location point, float size) {
        final Color color = Math.random() > 0.5 ? Color.fromRGB(184, 56, 207) : Color.fromRGB(174, 52, 179);
        Particle.DUST.builder()
                .location(point)
                .count(1)
                .extra(0.5)
                .data(new Particle.DustOptions(color, size))
                .receivers(60)
                .spawn();
    }

    public void remove() {
        display.remove();
    }

    /**
     * Plays the blade dissolving where it hung, for when it was never recalled to.
     */
    public void fade() {
        new SoundEffect(Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.7f, 0.5f).play(location);
        Particle.DUST.builder()
                .location(location)
                .count(15)
                .offset(0.2, 0.2, 0.2)
                .extra(0.5)
                .data(new Particle.DustOptions(Color.fromRGB(184, 56, 207), 1.3f))
                .receivers(60)
                .spawn();
    }
}
