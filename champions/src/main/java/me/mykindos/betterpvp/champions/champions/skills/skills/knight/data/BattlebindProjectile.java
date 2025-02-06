package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.Projectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class BattlebindProjectile extends Projectile {

    private final ItemDisplay display;
    private final LinkedHashMap<BlockDisplay, Double> chain = new LinkedHashMap<>();
    private final double damage;
    private final long pullTime;
    private final double pullSpeed;
    private LivingEntity target;
    private final Skill skill;

    public BattlebindProjectile(Player caster, double hitboxSize, Location location, long aliveTime, long pullTime, double pullSpeed, ItemStack sword, double damage, Skill skill) {
        super(caster, hitboxSize, location, aliveTime);
        this.damage = damage;
        this.skill = skill;
        this.pullTime = pullTime;
        this.pullSpeed = pullSpeed;
        this.display = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(sword);
            spawned.setGlowing(false);

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set(0.5, 0.5, 1.0);
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(90));
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(45));
            transformation.getLeftRotation().rotateLocalZ(0f);
            spawned.setTransformation(transformation);
            spawned.setPersistent(false);

            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
        });
    }

    @Override
    public boolean isExpired() {
        return UtilTime.elapsed(creationTime, impacted ? pullTime + aliveTime : aliveTime);
    }

    private Map.Entry<BlockDisplay, Double> appendChain(double speed) {
        final Location location = display.getLocation();
        final BlockDisplay block = location.getWorld().spawn(location, BlockDisplay.class, spawned -> {
            spawned.setBlock(Material.CHAIN.createBlockData());
            spawned.setGlowing(false);

            Transformation transformation = spawned.getTransformation();
            transformation.getTranslation().set(-0.5, 0.5, -0.5);
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(90));
            transformation.getLeftRotation().rotateLocalZ(0f);
            transformation.getScale().set(1, speed, 1);
            spawned.setTransformation(transformation);

            spawned.setPersistent(false);
            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
        });
        chain.putLast(block, 0d);
        return chain.lastEntry();
    }

    @Override
    protected void onTick() {
        final double speed = this.velocity.length() / 20;
        final Collection<Player> receivers = location.getNearbyPlayers(60);
        final BlockData data = Material.CHAIN.createBlockData();
        for (Location point : interpolateLine()) {
            Particle.BLOCK.builder()
                    .count(3)
                    .extra(0)
                    .data(data)
                    .offset(0.5, 0.5, 0.5)
                    .location(point)
                    .receivers(receivers)
                    .spawn();
        }

        display.teleport(location.clone().setDirection(display.getLocation().getDirection()));

        if (!impacted) {
            // start chain expanding chain following the sword
            Map.Entry<BlockDisplay, Double> toMove = this.chain.isEmpty() ? appendChain(speed) : this.chain.lastEntry();
            if (toMove.getValue() >= 1.0) {
                toMove = appendChain(speed);
            }

            this.chain.replace(toMove.getKey(), toMove.getValue(), toMove.getValue() + speed);
            final double progress = this.chain.get(toMove.getKey());
            toMove.getKey().getTransformation().getScale().mul(1, (float) progress, 1);
            // end chain

            location.getWorld().playSound(location, Sound.BLOCK_CHAIN_BREAK, 1f, 1f);
        } else if (!chain.isEmpty()) {
            if (target == null || !target.isValid()) {
                setMarkForRemoval(true);
                return;
            }

            // start chain retracting chain to the target
            Map.Entry<BlockDisplay, Double> toMove = chain.lastEntry();
            if (toMove.getValue() <= 0.0) {
                chain.pollLastEntry();
                toMove.getKey().remove();
                if (chain.isEmpty()) {
                    setMarkForRemoval(true);
                    return;
                }
                toMove = chain.lastEntry();
            }

            this.chain.replace(toMove.getKey(), toMove.getValue(), toMove.getValue() - pullSpeed / 20);
            final double progress = this.chain.get(toMove.getKey());
            toMove.getKey().getTransformation().getScale().mul(1, (float) progress, 1);
            // end chain

            // pull the target
            if (target != null) {
//                final Location tp = UtilLocation.shiftOutOfBlocks(target.getLocation().add(direction), target.getBoundingBox());
//                target.teleport(tp, TeleportFlag.Relative.PITCH, TeleportFlag.Relative.YAW);
                final VelocityData velocity = new VelocityData(this.velocity.clone().normalize(), pullSpeed / 20, 0, 10, false);
                UtilVelocity.velocity(target, caster, velocity);
            }
            // end pull

            location.getWorld().playSound(location, Sound.BLOCK_CHAIN_BREAK, 1f, 0f);
        } else {
            setMarkForRemoval(true);
        }
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        // hit entities can only be living entities (non-armorstands) by default
        final Entity hit = result.getHitEntity();
        Particle.BLOCK.builder()
                .count(100)
                .extra(0)
                .data(Material.NETHERITE_BLOCK.createBlockData())
                .offset(0.5, 0.5, 0.5)
                .location(location)
                .receivers(30)
                .spawn();
        caster.playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 0.5F);

        if (hit == null) {
            setMarkForRemoval(true); // only remove if no hit entity, we will do that once the entity is pulled
            return;
        }

        redirect(getVelocity().clone().normalize().multiply(-1).multiply(pullSpeed));

        final CustomDamageEvent event = new CustomDamageEvent(((LivingEntity) hit),
                caster,
                null,
                EntityDamageEvent.DamageCause.CUSTOM,
                damage,
                true,
                skill.getName());
        event.setForceDamageDelay(0);
        event.setDamageDelay(0);
        UtilDamage.doCustomDamage(event);
        target = (LivingEntity) hit;

        UtilMessage.simpleMessage(hit, skill.getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), skill.getName());
        UtilMessage.simpleMessage(caster, skill.getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s</alt>.", hit.getName(), skill.getName());
    }

    public void remove() {
        display.remove();
        for (BlockDisplay block : chain.keySet()) {
            block.remove();
        }
    }
}