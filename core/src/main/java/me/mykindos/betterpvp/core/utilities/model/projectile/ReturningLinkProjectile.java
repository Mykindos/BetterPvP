package me.mykindos.betterpvp.core.utilities.model.projectile;

import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.RayTraceResult;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ReturningLinkProjectile extends Projectile {

    protected final Display lead;
    private final LinkedHashMap<Display, Double> links = new LinkedHashMap<>();
    private final long pullTime;
    private final double pullSpeed;
    protected LivingEntity target;

    protected ReturningLinkProjectile(Player caster, double hitboxSize, Location location, long aliveTime, long pullTime, double pullSpeed) {
        super(caster, hitboxSize, location, aliveTime);
        this.pullTime = pullTime;
        this.pullSpeed = pullSpeed;
        this.lead = item();
    }

    public boolean hasFinishedPulling() {
        return impacted && links.isEmpty();
    }

    @Override
    public boolean isExpired() {
        return UtilTime.elapsed(creationTime, impacted ? pullTime + aliveTime : aliveTime);
    }

    protected abstract Display item();

    protected abstract Display createLink(double height);

    private Map.Entry<Display, Double> appendLink(double speed) {
        links.putLast(createLink(speed), 0d);
        return links.lastEntry();
    }

    @Override
    protected boolean canCollideWith(Entity entity) {
        if (!super.canCollideWith(entity)) {
            return false;
        }

        final EntityCanHurtEntityEvent event = new EntityCanHurtEntityEvent(caster, (LivingEntity) entity);
        event.callEvent();
        return event.getResult() != Event.Result.DENY;
    }

    @Override
    protected void onTick() {
        final double speed = this.velocity.length() / 20;
        lead.teleport(location.clone().setDirection(lead.getLocation().getDirection()));

        if (!impacted) {

            // start link expanding link following the sword
            double remaining = speed;
            while (remaining > 0) {
                Map.Entry<Display, Double> toMove = this.links.isEmpty() ? appendLink(speed) : this.links.lastEntry();
                if (toMove.getValue() >= 1.0) {
                    toMove = appendLink(speed);
                }

                final double progress = toMove.getValue() + remaining <= 1.0 ? remaining : 1.0 - toMove.getValue();
                this.links.replace(toMove.getKey(), toMove.getValue(), toMove.getValue() + progress);
                toMove.getKey().getTransformation().getScale().mul(1, (float) progress, 1);
                remaining -= progress;
            }
            // end link

            pushSound().play(location);
        } else if (!links.isEmpty()) {
            if (target == null || !target.isValid()) {
                setMarkForRemoval(true);
                return;
            }

            // start link retracting link to the target
            double remaining = pullSpeed / 20.;
            while (remaining > 0) {
                Map.Entry<Display, Double> toMove = links.lastEntry();
                if (toMove.getValue() <= 0.0) {
                    links.pollLastEntry();
                    toMove.getKey().remove();
                    if (links.isEmpty()) {
                        setMarkForRemoval(true);
                        return;
                    }
                    toMove = links.lastEntry();
                }

                final double progress = toMove.getValue() - remaining >= 0.0 ? remaining : toMove.getValue();
                this.links.replace(toMove.getKey(), toMove.getValue(), toMove.getValue() - progress);
                toMove.getKey().getTransformation().getScale().mul(1, (float) progress, 1);
                remaining -= progress;
                // end link

                // pull the target
                if (target != null) {
//                final Location tp = UtilLocation.shiftOutOfBlocks(target.getLocation().add(direction), target.getBoundingBox());
//                target.teleport(tp, TeleportFlag.Relative.PITCH, TeleportFlag.Relative.YAW);
                    final VelocityData velocity = new VelocityData(this.velocity.clone().normalize(), pullSpeed / 20, 0, 100, false);
                    UtilVelocity.velocity(target, caster, velocity);
                    target.setFallDistance(0);
                }
            }
            // end pull

            pullSound().play(location);
        } else {
            setMarkForRemoval(true);
        }
    }

    protected abstract SoundEffect pullSound();

    protected abstract SoundEffect pushSound();

    protected abstract SoundEffect impactSound();

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
        impactSound().play(location);

        if (hit == null) {
            setMarkForRemoval(true); // only remove if no hit entity, we will do that once the entity is pulled
            return;
        }

        redirect(getVelocity().clone().normalize().multiply(-1).multiply(pullSpeed));

        target = (LivingEntity) hit;
    }

    public void remove() {
        lead.remove();
        for (Display link : links.keySet()) {
            link.remove();
        }
    }
}