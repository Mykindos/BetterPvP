package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scepter;

import lombok.Getter;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.model.RayProjectile;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

@Getter
public class MeridianBeam extends RayProjectile {

    public static final String NAME = "Meridian Beam";
    private final double damage;

    public MeridianBeam(Player caster, final Location location, double hitboxSize, double size, long expireTime, double damage) {
        super(caster, hitboxSize, size, location, expireTime);
        this.damage = damage;
    }

    @Override
    protected void onTick() {
        for (Location point : interpolateLine()) {
            // Play travel particles
            final Color color = Math.random() > 0.5 ? Color.fromRGB(184, 56, 207) : Color.fromRGB(174, 52, 179);
            Particle.DUST.builder()
                    .location(point)
                    .count(1)
                    .extra(0.5)
                    .data(new Particle.DustOptions(color, 1.3f))
                    .receivers(60)
                    .spawn();
        }
    }

    @Override
    public void redirect(Vector vector) {
        super.redirect(vector);
        new SoundEffect(Sound.ENTITY_SHULKER_BULLET_HIT, 0f, 1f).play(location);
        new SoundEffect(Sound.ENTITY_SHULKER_BULLET_HIT, 2f, 1f).play(location);
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        markForRemoval = true;
        final Entity entity = result.getHitEntity();
        if (entity instanceof ArmorStand || !(entity instanceof LivingEntity target)) {
            return;
        }

        final CustomDamageEvent event = new CustomDamageEvent(
                target,
                caster,
                caster,
                EntityDamageEvent.DamageCause.PROJECTILE,
                damage,
                true,
                NAME
        );
        UtilDamage.doCustomDamage(event);

        if (!event.isCancelled() && (caster != null && caster.isOnline())) {
            new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2f, 0.5f).play(caster);
        }
    }
}
