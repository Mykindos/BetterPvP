package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.Projectile;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

@Getter
public class AxeProjectile extends Projectile {

    private final ItemDisplay display;
    private final ItemStack itemStack;
    private final double damage;
    private final Skill skill;
    private final float yaw;
    private final double speed;

    public AxeProjectile(Player caster, double hitboxSize, Location location, long aliveTime, ItemStack axe, double damage, double speed, Skill skill) {
        super(caster, hitboxSize, location, aliveTime);
        this.damage = damage;
        this.skill = skill;
        this.yaw = caster.getLocation().getYaw();
        this.itemStack = axe.clone();
        this.speed = speed;
        this.gravity = Projectile.DEFAULT_GRAVITY;
        this.dragCoefficient = Projectile.DEFAULT_DRAG_COEFFICIENT;

        this.display = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(axe);
            spawned.setGlowing(false);
            spawned.setPersistent(false);

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set(0.75, 0.75, 0.75);
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(-yaw - 90));
            spawned.setTransformation(transformation);
            spawned.setInterpolationDuration(1);
            spawned.setTeleportDuration(1);
        });
    }

    public void remove() {
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    @Override
    protected void onTick() {
        if (isImpacted()) {
            location.getWorld().playSound(location, Sound.ENTITY_BREEZE_INHALE, 0.2F, 2.0F);
            Particle.ENCHANTED_HIT.builder()
                    .count(3)
                    .extra(0)
                    .offset(0.1, 0.1, 0.1)
                    .location(location)
                    .receivers(60)
                    .spawn();

            redirect(caster.getEyeLocation().subtract(location).toVector().normalize().multiply(speed));
        } else {
            location.getWorld().playSound(location, Sound.ITEM_BUNDLE_INSERT, 0.5F, 1.0F);
            Particle.CRIT.builder()
                    .count(3)
                    .extra(0)
                    .offset(0.1, 0.1, 0.1)
                    .location(location)
                    .receivers(60)
                    .spawn();
        }

        display.teleport(location.clone().setDirection(display.getLocation().getDirection()));

        Transformation transformation = display.getTransformation();
        final float elapsedTotal = System.currentTimeMillis() - creationTime;
        float pitch = (float) ((elapsedTotal / 1000) / 10f * (-360.0 * 5));
        Vector3f axis = new Vector3f(0, 0, 1);
        AxisAngle4f pitchRotation = new AxisAngle4f((float) Math.toRadians(pitch), axis);
        transformation.getLeftRotation().set(pitchRotation);
        transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(90));
        display.setTransformation(transformation);
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        final Entity hitEntity = result.getHitEntity();
        if (!(hitEntity instanceof LivingEntity damagee)) {
            final Block block = result.getHitBlock();
            if (block != null) {
                location.getWorld().playSound(location, block.getBlockSoundGroup().getBreakSound(), 1.0F, 2.0F);
                location.getWorld().playSound(location, block.getBlockSoundGroup().getBreakSound(), 1.0F, 0.0F);
                Particle.BLOCK.builder()
                        .count(30)
                        .extra(1)
                        .offset(0.2, 0.2, 0.2)
                        .data(block.getBlockData())
                        .location(location)
                        .receivers(60)
                        .spawn();
            }
            return;
        }

        damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 2.0F, 1.0F);
        UtilDamage.doCustomDamage(new CustomDamageEvent(damagee, caster, null, EntityDamageEvent.DamageCause.CUSTOM, damage, true, skill.getName()));
        UtilMessage.simpleMessage(caster, skill.getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s</alt>.", damagee.getName(), skill.getName());
        UtilMessage.simpleMessage(damagee, skill.getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), skill.getName());
    }
}
