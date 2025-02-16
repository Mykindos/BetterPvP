package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.model.Projectile;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

public class LeechSoulProjectile extends Projectile {

    private final double speed;
    private final double health;

    public LeechSoulProjectile(Player caster, double hitboxSize, Location location, long aliveTime, double speed, double health) {
        super(caster, hitboxSize, location, aliveTime);
        this.speed = speed;
        this.health = health;
    }

    @Override
    protected void onTick() {
        if (Bukkit.getCurrentTick() % 2 == 0) {
            new SoundEffect(Sound.BLOCK_LAVA_POP, 1.3f, 2f).play(location);
            Particle.HEART.builder()
                    .count(1)
                    .extra(0)
                    .location(location)
                    .receivers(60)
                    .spawn();
        }

        redirect(caster.getEyeLocation().subtract(location.toVector()).toVector().normalize().multiply(speed));
    }

    @Override
    protected boolean canCollideWith(Entity entity) {
        return entity == caster;
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        // was DEFINITELY the caster
        UtilPlayer.slowHealth(JavaPlugin.getPlugin(Champions.class), caster, health, (int) health * 2, true);
        markForRemoval = true; // ONCE
    }
}
