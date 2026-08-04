package me.mykindos.betterpvp.core.utilities.model.projectile;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A projectile that trails a chain of {@link Display} links behind its lead entity, growing the chain
 * as it flies. What happens to the chain once the projectile lands is left to subclasses - it can be
 * retracted ({@link ReturningLinkProjectile}), snapped, or left hanging.
 */
public abstract class LinkProjectile extends Projectile {

    protected final Display lead;

    /**
     * Every spawned link mapped to how far it has grown, from 0 to 1. Its full world-space length is
     * stored on the display itself under the {@code height} metadata key.
     */
    protected final LinkedHashMap<Display, Double> links = new LinkedHashMap<>();

    protected LivingEntity hit;
    protected Location impactLocation;

    protected LinkProjectile(Player caster, double hitboxSize, Location location, long aliveTime) {
        super(caster, hitboxSize, location, aliveTime);
        this.lead = item();
    }

    protected abstract Display item();

    protected abstract Display createLink(Location spawnLocation, double height);

    protected abstract SoundEffect pushSound();

    protected abstract SoundEffect impactSound();

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
        lead.teleport(location.clone().setDirection(lead.getLocation().getDirection()));

        if (!impacted) {
            tickOutgoing(this.velocity.length() / 20);
        }
    }

    /**
     * Grows the chain by one tick's worth of travel, appending new links as the previous one fills up.
     */
    protected void tickOutgoing(double speed) {
        double remaining = speed;

        while (remaining > 0) {
            Map.Entry<Display, Double> toMove;

            if (this.links.isEmpty()) {
                toMove = appendLink(this.location.clone(), speed);
            } else {
                toMove = this.links.lastEntry();
                if (toMove.getValue() >= 1.0) {
                    Location newLinkStart = getEndOfLink(toMove.getKey());
                    toMove = appendLink(newLinkStart, speed);
                }
            }

            final double availableProgress = Math.max(0.05, 1.0 - toMove.getValue());
            final double progress = Math.min(remaining, availableProgress);
            final double newProgress = toMove.getValue() + progress;
            this.links.replace(toMove.getKey(), toMove.getValue(), newProgress);

            final double height = toMove.getKey().getMetadata("height").getFirst().asDouble();
            remaining -= Math.max(0.05, progress * height);
            final Transformation transformation = toMove.getKey().getTransformation();
            transformation.getScale().set(
                    transformation.getScale().x,
                    (float) (height * newProgress),
                    transformation.getScale().z
            );
            toMove.getKey().setTransformation(transformation);
            toMove.getKey().teleport(toMove.getKey().getLocation().setDirection(getVelocity()));
        }

        pushSound().play(location);
    }

    protected Map.Entry<Display, Double> appendLink(Location spawnLocation, double height) {
        Display link = createLink(spawnLocation, height);
        link.setMetadata("height", new FixedMetadataValue(
                JavaPlugin.getPlugin(Core.class), link.getTransformation().getScale().y));
        links.putLast(link, 0d);
        return links.lastEntry();
    }

    protected Location getEndOfLink(Display link) {
        double height = link.getMetadata("height").getFirst().asDouble();
        Vector direction = getVelocity().clone().normalize();
        return link.getLocation().clone().add(direction.multiply(height));
    }

    /**
     * Updates the visual scale of a link based on its current progress value.
     */
    protected void updateLinkScale(Display link, double progressValue) {
        if (!link.hasMetadata("height")) return;

        final double originalHeight = link.getMetadata("height").getFirst().asDouble();
        final Transformation transformation = link.getTransformation();
        transformation.getScale().set(
                transformation.getScale().x,
                (float) (originalHeight * progressValue),
                transformation.getScale().z
        );
        link.setTransformation(transformation);
    }

    protected void removeLinks() {
        for (Display link : links.keySet()) {
            link.remove();
        }
        links.clear();
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        this.impactLocation = location.clone();
        impactSound().play(location);

        final Entity hit = result.getHitEntity();
        if (hit != null) {
            this.hit = (LivingEntity) hit;
        }
    }

    public void remove() {
        lead.remove();
        removeLinks();
    }
}
