package me.mykindos.betterpvp.champions.champions.skills.skills.brute.data;

import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.Projectile;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

@Getter
public class SkullsplitterProjectile extends Projectile {

    private final ItemDisplay display;
    private final ItemStack itemStack;
    private final double bleedSeconds;
    private final Skill skill;
    private final float yaw;
    private final double speed;
    private final EffectManager effectManager;

    public SkullsplitterProjectile(Player caster, double hitboxSize, Location location, long aliveTime, ItemStack axe, double bleedSeconds, double speed, Skill skill, EffectManager effectManager) {
        super(caster, hitboxSize, location, aliveTime);
        this.bleedSeconds = bleedSeconds;
        this.skill = skill;
        this.yaw = caster.getLocation().getYaw();
        this.itemStack = axe.clone();
        this.speed = speed;
        this.effectManager = effectManager;
        this.gravity = Projectile.DEFAULT_GRAVITY;
        this.dragCoefficient = Projectile.DEFAULT_DRAG_COEFFICIENT;

        this.display = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(axe);
            spawned.setGlowing(false);
            spawned.setBrightness(new Display.Brightness(15, 15));
            spawned.setPersistent(false);

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set(1.9, 1.9, 1.9);
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(-yaw - 90));
            spawned.setTransformation(transformation);
            spawned.setInterpolationDuration(1);
            spawned.setTeleportDuration(1);
        });
    }

    @Override
    public boolean isExpired() {
        // allow for the same amount of time to pass after impacting so a player can follow the path
        return isImpacted() ? UtilTime.elapsed(impactTime, aliveTime) : super.isExpired();
    }

    public void remove() {
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    @Override
    protected void onTick() {
        display.teleport(location.clone().setDirection(display.getLocation().getDirection()));

        // rotation
        if (!isImpacted()) {
            location.getWorld().playSound(location, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.4F, 0.3F);
            Particle.CRIT.builder()
                    .count(3)
                    .extra(0)
                    .offset(0.1, 0.1, 0.1)
                    .location(location)
                    .receivers(60)
                    .spawn();

            final float elapsedTotal = System.currentTimeMillis() - creationTime;
            float pitch = (float) ((elapsedTotal / 1000) / 10f * (-360.0 * 5));
            Vector3f axis = new Vector3f(0, 0, 1);
            AxisAngle4f pitchRotation = new AxisAngle4f((float) Math.toRadians(pitch), axis);
            Transformation transformation = display.getTransformation();
            transformation.getLeftRotation().set(pitchRotation);
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(90));
            display.setTransformation(transformation);
        } else if (caster.isValid()) {
            // grace period of 300 millis to allow the player to left up the ground
            if (UtilTime.elapsed(impactTime, 300L) && UtilBlock.isGrounded(caster)) {
                setMarkForRemoval(true);
                Particle.SMOKE.builder()
                        .count(40)
                        .offset(0.5, 0.5, 0.5)
                        .extra(0)
                        .location(location)
                        .receivers(60)
                        .spawn();

                // cues
                Particle.CLOUD.builder()
                        .count(20)
                        .extra(0.1)
                        .offset(0.4, 0.4, 0.4)
                        .location(caster.getLocation())
                        .receivers(60)
                        .spawn();
                location.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1F, 0.3F);
                location.getWorld().playSound(location, Sound.BLOCK_LAVA_POP, 1.2F, 0.3F);
                final Block block = caster.getLocation().getBlock().getRelative(0, -1, 0);
                location.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
            }
        }
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        final Entity hitEntity = result.getHitEntity();

        // boost player
        final boolean direct = hitEntity instanceof LivingEntity;
        final Location target = direct ? hitEntity.getLocation() : location;
        effectManager.addEffect(caster, caster, EffectTypes.NO_FALL, skill.getName(), 9999, 1000, true, true, UtilBlock::isGrounded);
        final Vector vector = target.clone().subtract(caster.getLocation()).toVector();
        if (direct) {
            vector.multiply(new Vector(1, 0.5, 1));
        }

        final VelocityData data = new VelocityData(vector, speed / 15, 0.5, 1.2, true);
        UtilVelocity.velocity(caster, caster, data);
        // end boost player

        // cues & bleed
        if (!(hitEntity instanceof LivingEntity damagee)) {
            // cancel movement of the axe if it hit a block so it looks like its stuck
            redirect(new Vector(0, 0, 0)); // stay in place
            gravity = new Vector(0, 0, 0); // no gravity
            dragCoefficient = 0.0; // no drag
            // end cancel movement

            final Block block = result.getHitBlock();
            if (block != null) {
                location.getWorld().playSound(location, block.getBlockSoundGroup().getBreakSound(), 1.4F, 0.0F);
                location.getWorld().playSound(location, block.getBlockSoundGroup().getBreakSound(), 1.4F, 2.0F);
                Particle.BLOCK.builder()
                        .count(120)
                        .extra(1)
                        .offset(0.8, 0.8, 0.8)
                        .data(block.getBlockData())
                        .location(location)
                        .receivers(60)
                        .spawn();
            }
            return;
        }

        remove(); // remove the axe if it hit a player

        final EntityCanHurtEntityEvent event = new EntityCanHurtEntityEvent(caster, damagee);
        event.callEvent();

        damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 2.0F, 0.0F);
        if (event.getResult() != Event.Result.DENY) {
            this.effectManager.addEffect(damagee, caster, EffectTypes.BLEED, 1, (long) (bleedSeconds * 1000));
            UtilDamage.doCustomDamage(new CustomDamageEvent(damagee, caster, null, EntityDamageEvent.DamageCause.CUSTOM, bleedSeconds, true, skill.getName()));
            UtilMessage.simpleMessage(caster, skill.getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s</alt>.", damagee.getName(), skill.getName());
            UtilMessage.simpleMessage(damagee, skill.getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), skill.getName());
        }
    }
}
