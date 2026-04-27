package me.mykindos.betterpvp.progression.profession.mining.item.projectile;

import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import me.mykindos.betterpvp.progression.profession.mining.item.interaction.ExplosiveExcavationInteraction;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

@Getter
public class RiftPickaxeProjectile extends Projectile {

    private static final Set<Material> UNBREAKABLE_TYPES = EnumSet.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN
    );

    private boolean recalled = false;
    private long recallTime = 0;
    private long lastExplosionTime;
    private int bounces = 0;

    private final int explosionRadius;
    private final long explosionIntervalMillis;
    private final double oreChance;
    private final int maxBounces;
    private final ItemDisplay itemDisplay;
    private final Supplier<Material> oreSupplier;

    public RiftPickaxeProjectile(
            Player caster,
            double hitboxSize,
            Location location,
            long aliveTime,
            ItemStack displayItem,
            int explosionRadius,
            long explosionIntervalMillis,
            double oreChance,
            int maxBounces,
            Supplier<Material> oreSupplier) {
        super(caster, hitboxSize, location, aliveTime);
        this.explosionRadius = explosionRadius;
        this.explosionIntervalMillis = explosionIntervalMillis;
        this.oreChance = oreChance;
        this.maxBounces = maxBounces;
        this.oreSupplier = oreSupplier;
        this.lastExplosionTime = creationTime;

        this.itemDisplay = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(displayItem);
            spawned.setPersistent(false);
            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
            spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
        });
    }

    @Override
    public boolean isExpired() {
        return recalled ? UtilTime.elapsed(recallTime, 10_000L) : super.isExpired();
    }

    public void recall() {
        if (recalled) return;
        recalled = true;
        recallTime = System.currentTimeMillis();
        new SoundEffect(Sound.ITEM_TOTEM_USE, 2.0f, 0.8f).play(getLocation());
    }

    @Override
    protected void onTick() {
        // Update the item display entity position
        itemDisplay.teleport(location.clone().setDirection(getVelocity()));

        // Spawn trail particles along the interpolated line
        final Collection<Player> receivers = location.getNearbyPlayers(60);
        for (Location point : interpolateLine()) {
            Particle.END_ROD.builder()
                    .count(1)
                    .extra(0.1)
                    .offset(0.05, 0.05, 0.05)
                    .location(point)
                    .receivers(receivers)
                    .spawn();
            Particle.CRIT.builder()
                    .count(2)
                    .offset(0.1, 0.1, 0.1)
                    .location(point)
                    .receivers(receivers)
                    .spawn();
        }

        // Fire explosion at interval
        long now = System.currentTimeMillis();
        if (now - lastExplosionTime >= explosionIntervalMillis && UtilBlock.isUnderground(location)) {
            ExplosiveExcavationInteraction.detonate(caster, location, explosionRadius, oreChance, oreSupplier);
            lastExplosionTime = now;
        }

        if (!recalled) {
            new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 1.2f, 0.4f).play(getLocation());
            return;
        }

        // Recalled: move toward caster's midpoint
        final Location playerMidpoint = UtilPlayer.getMidpoint(caster);
        if (location.distanceSquared(playerMidpoint) < 1.0) {
            setMarkForRemoval(true);
            return;
        }

        final double speed = getVelocity().length();
        final Vector direction = playerMidpoint.toVector()
                .subtract(location.toVector())
                .normalize();
        redirect(direction.multiply(speed));
    }

    @Override
    protected CollisionResult onCollide(RayTraceResult result) {
        final Entity entity = result.getHitEntity();
        if (entity != null) {
            // Mining tool, not a weapon — pass through entities
            return CollisionResult.CONTINUE;
        }

        // Check if we hit an unbreakable surface
        final Block hitBlock = result.getHitBlock();
        if (hitBlock != null) {
            ExplosiveExcavationInteraction.detonate(caster, location, explosionRadius, oreChance, oreSupplier);
            bounces++;
            // After exhausting bounces, stop reflecting and start the return flight so the
            // projectile cleans itself up via the existing recall path instead of pinballing.
            if (bounces >= maxBounces) {
                recall();
                return CollisionResult.CONTINUE;
            }
            return CollisionResult.REFLECT_BLOCKS;
        }

        // Otherwise continue; explosions handle block destruction
        return CollisionResult.CONTINUE;
    }

    public void remove() {
        itemDisplay.remove();
        Particle.GUST.builder()
                .count(1)
                .extra(0.3)
                .offset(0.3, 0.3, 0.3)
                .location(location)
                .receivers(60)
                .spawn();
        Particle.FLASH.builder()
                .count(1)
                .color(Color.AQUA)
                .extra(0.1)
                .location(location)
                .receivers(60)
                .spawn();
    }
}
