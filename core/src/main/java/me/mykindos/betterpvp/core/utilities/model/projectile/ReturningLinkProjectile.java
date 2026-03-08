package me.mykindos.betterpvp.core.utilities.model.projectile;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
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

public abstract class ReturningLinkProjectile extends Projectile {

    /**
     * Defines how the chain retracts after impact.
     */
    public enum PullMode {
        /**
         * Chain retracts from the end only,
         * pulling the target toward the caster's position.
         */
        PULL_TARGET,

        /**
         * Chain retracts from the start only,
         * pulling the caster toward the target/impact point.
         * Works with or without hitting an entity (can grapple to walls).
         */
        PULL_CASTER,

        /**
         * Chain retracts from both ends simultaneously,
         * pulling both caster and target toward the midpoint.
         */
        PULL_TO_MIDPOINT
    }

    protected final Display lead;
    private final LinkedHashMap<Display, Double> links = new LinkedHashMap<>();
    private final long pullTime;
    private final double pullSpeed;
    private final double meetDistance;

    @Setter
    @Getter
    protected PullMode pullMode;
    protected LivingEntity hit;
    protected Location anchorLocation; // Where the chain started (caster position at impact)
    protected Location impactLocation; // Where the projectile hit (for wall grappling)

    /**
     * Constructor with default PULL_TARGET mode for backwards compatibility.
     */
    protected ReturningLinkProjectile(Player caster, double hitboxSize, Location location, long aliveTime, long pullTime, double pullSpeed) {
        this(caster, hitboxSize, location, aliveTime, pullTime, pullSpeed, PullMode.PULL_TARGET, 0.5);
    }

    /**
     * Constructor with configurable pull mode.
     *
     * @param caster       The player who cast the projectile
     * @param hitboxSize   Size of the hitbox for collision detection
     * @param location     Starting location of the projectile
     * @param aliveTime    How long the projectile can travel before expiring
     * @param pullTime     How long the pull phase can last
     * @param pullSpeed    Speed at which entities are pulled
     * @param pullMode     The retraction behavior mode
     * @param meetDistance Distance at which entities are considered "met" (for PULL_TO_MIDPOINT)
     */
    protected ReturningLinkProjectile(Player caster, double hitboxSize, Location location, long aliveTime,
                                      long pullTime, double pullSpeed, PullMode pullMode, double meetDistance) {
        super(caster, hitboxSize, location, aliveTime);
        this.pullTime = pullTime;
        this.pullSpeed = pullSpeed;
        this.pullMode = pullMode;
        this.meetDistance = meetDistance;
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

    protected abstract Display createLink(Location spawnLocation, double height);

    private Map.Entry<Display, Double> appendLink(Location spawnLocation, double height) {
        Display link = createLink(spawnLocation, height);
        link.setMetadata("height", new FixedMetadataValue(
                JavaPlugin.getPlugin(Core.class), link.getTransformation().getScale().y));
        links.putLast(link, 0d);
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
        final double length = this.velocity.length();
        final double speed = length / 20;
        lead.teleport(location.clone().setDirection(lead.getLocation().getDirection()));

        if (!impacted) {
            tickOutgoing(speed);
        } else if (!links.isEmpty()) {
            // PULL_CASTER doesn't require a hit entity (can grapple to walls)
            // Other modes require a valid hit entity
            if (pullMode != PullMode.PULL_CASTER && (hit == null || !hit.isValid())) {
                setMarkForRemoval(true);
                return;
            }

            switch (pullMode) {
                case PULL_TARGET -> tickPullTarget();
                case PULL_CASTER -> tickPullCaster();
                case PULL_TO_MIDPOINT -> tickPullToMidpoint();
            }
        }
    }

    /**
     * Handles the outgoing phase where the chain extends toward the target.
     */
    private void tickOutgoing(double speed) {
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

    private Location getEndOfLink(Display link) {
        double height = link.getMetadata("height").getFirst().asDouble();
        Vector direction = getVelocity().clone().normalize();
        return link.getLocation().clone().add(direction.multiply(height));
    }

    /**
     * Retracts chain from the end, pulling target to caster.
     */
    private void tickPullTarget() {
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

            redirect(toMove.getKey().getLocation().subtract(location).toVector().normalize().multiply(pullSpeed));

            final double height = toMove.getKey().getMetadata("height").getFirst().asDouble();
            final double progressNeeded = remaining / height;
            final double progress = Math.min(progressNeeded, toMove.getValue());
            double newValue = toMove.getValue() - progress;
            this.links.replace(toMove.getKey(), toMove.getValue(), newValue);
            updateLinkScale(toMove.getKey(), newValue);
            remaining -= progress * height;

            // Pull the target toward caster
            if (hit != null) {
                final VelocityData velocity = new VelocityData(this.velocity.clone().normalize(), pullSpeed / 20, 0, 100, true);
                UtilVelocity.velocity(hit, caster, velocity);
                hit.setFallDistance(0);
            }
        }

        pullSound().play(location);
    }

    /**
     * Retracts chain from the start, pulling caster toward the target/impact point.
     * Works with or without hitting an entity (can grapple to walls).
     */
    private void tickPullCaster() {
        Preconditions.checkNotNull(caster, "Caster entity cannot be null");
        if (!caster.isValid()) {
            setMarkForRemoval(true);
            return;
        }

        // Determine the pull destination (entity location or wall impact point)
        Location destination = (hit != null && hit.isValid())
                ? hit.getLocation().add(0, 1, 0)
                : impactLocation;

        // Check if caster has arrived at destination
        double distance = caster.getLocation().distance(destination);
        if (distance <= meetDistance) {
            setMarkForRemoval(true);
            return;
        }

        // Retract chain from the START (caster side)
        double remaining = pullSpeed / 20.;
        while (remaining > 0 && !links.isEmpty()) {
            Map.Entry<Display, Double> toMove = links.firstEntry();

            if (toMove.getValue() <= 0.0) {
                links.pollFirstEntry();
                toMove.getKey().remove();
                if (links.isEmpty()) {
                    setMarkForRemoval(true);
                    return;
                }
                toMove = links.firstEntry();
            }

            final double height = toMove.getKey().getMetadata("height").getFirst().asDouble();
            final double progressNeeded = remaining / height;
            final double progress = Math.min(progressNeeded, toMove.getValue());
            double newValue = toMove.getValue() - progress;
            links.replace(toMove.getKey(), toMove.getValue(), newValue);
            updateLinkScale(toMove.getKey(), newValue);
            remaining -= progress * height;
        }

        if (links.isEmpty()) {
            setMarkForRemoval(true);
            return;
        }

        // Reposition remaining links between caster and destination
        repositionLinksToDestination(destination);

        // Pull CASTER toward destination
        Vector casterToDestination = destination.toVector()
                .subtract(caster.getLocation().toVector())
                .normalize();
        VelocityData casterVel = new VelocityData(casterToDestination, pullSpeed / 20, 0, 100, true);
        UtilVelocity.velocity(caster, null, casterVel);
        caster.setFallDistance(0);

        pullSound().play(caster.getLocation());
    }

    /**
     * Repositions links between caster and a fixed destination point.
     * Used by PULL_CASTER mode.
     */
    private void repositionLinksToDestination(Location destination) {
        if (links.isEmpty()) return;

        Location start = caster.getLocation().add(0, 1, 0); // Chest height
        Location end = destination;
        Vector direction = end.toVector().subtract(start.toVector());
        double totalDistance = direction.length();
        direction.normalize();

        int linkCount = links.size();
        double spacing = totalDistance / (linkCount + 1);

        int index = 1;
        for (Map.Entry<Display, Double> entry : links.entrySet()) {
            Display link = entry.getKey();

            // Position this link along the chain
            Location linkPos = start.clone().add(direction.clone().multiply(spacing * index));
            linkPos.setDirection(direction);
            link.teleport(linkPos);

            index++;
        }

        // Position the lead at the destination
        lead.teleport(end.clone().setDirection(direction.clone().multiply(-1)));
    }

    /**
     * Retracts chain from both ends, pulling both entities to midpoint.
     */
    private void tickPullToMidpoint() {
        Preconditions.checkNotNull(caster, "Caster entity cannot be null");
        if (!caster.isValid() || hit == null || !hit.isValid()) {
            setMarkForRemoval(true);
            return;
        }

        // Check if entities have met
        double distance = caster.getLocation().distance(hit.getLocation());
        if (distance <= meetDistance) {
            setMarkForRemoval(true);
            return;
        }

        // Calculate the current midpoint between caster and target
        Location midpoint = caster.getLocation().clone()
                .add(hit.getLocation())
                .multiply(0.5);

        // Split the retraction speed between both ends
        double speedPerEnd = pullSpeed / 20.; // Half speed for each end

        // Retract from the END (target side) - shrinking links from the back
        retractFromEnd(speedPerEnd);

        // Retract from the START (caster side) - shrinking links from the front
        retractFromStart(speedPerEnd);

        // Reposition remaining links along the chain between caster and target
        repositionLinks();

        // Pull TARGET toward midpoint
        Vector targetToMid = midpoint.toVector()
                .subtract(hit.getLocation().toVector())
                .normalize();
        VelocityData targetVel = new VelocityData(targetToMid, pullSpeed / 20, 0, 100, true);
        UtilVelocity.velocity(hit, caster, targetVel);
        hit.setFallDistance(0);

        // Pull CASTER toward midpoint
        Vector casterToMid = midpoint.toVector()
                .subtract(caster.getLocation().toVector())
                .normalize();
        VelocityData casterVel = new VelocityData(casterToMid, pullSpeed / 20, 0, 100, true);
        UtilVelocity.velocity(caster, null, casterVel);
        caster.setFallDistance(0);

        // Play sound at midpoint
        pullSound().play(midpoint);
    }

    /**
     * Retracts links from the end of the chain (target side).
     */
    private void retractFromEnd(double speed) {
        double remaining = speed;
        while (remaining > 0 && !links.isEmpty()) {
            Map.Entry<Display, Double> toMove = links.lastEntry();

            if (toMove.getValue() <= 0.0) {
                links.pollLastEntry();
                toMove.getKey().remove();
                if (links.isEmpty()) return;
                toMove = links.lastEntry();
            }

            final double height = toMove.getKey().getMetadata("height").getFirst().asDouble();
            final double progressNeeded = remaining / height;
            final double progress = Math.min(progressNeeded, toMove.getValue());
            double newValue = toMove.getValue() - progress;
            links.replace(toMove.getKey(), toMove.getValue(), newValue);
            updateLinkScale(toMove.getKey(), newValue);
            remaining -= progress * height;
        }
    }

    /**
     * Retracts links from the start of the chain (caster side).
     */
    private void retractFromStart(double speed) {
        double remaining = speed;
        while (remaining > 0 && !links.isEmpty()) {
            Map.Entry<Display, Double> toMove = links.firstEntry();

            if (toMove.getValue() <= 0.0) {
                links.pollFirstEntry();
                toMove.getKey().remove();
                if (links.isEmpty()) return;
                toMove = links.firstEntry();
            }

            final double height = toMove.getKey().getMetadata("height").getFirst().asDouble();
            final double progressNeeded = remaining / height;
            final double progress = Math.min(progressNeeded, toMove.getValue());
            double newValue = toMove.getValue() - progress;
            links.replace(toMove.getKey(), toMove.getValue(), newValue);
            updateLinkScale(toMove.getKey(), newValue);
            remaining -= progress * height;
        }
    }

    /**
     * Updates the visual scale of a link based on its current progress value.
     */
    private void updateLinkScale(Display link, double progressValue) {
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

    /**
     * Repositions all links evenly along the chain between caster and target.
     * This keeps the visual chain connected as both ends retract.
     */
    private void repositionLinks() {
        if (links.isEmpty()) return;

        Location start = caster.getLocation().add(0, 1, 0); // Chest height
        Location end = hit.getLocation().add(0, 1, 0);
        Vector direction = end.toVector().subtract(start.toVector());
        double totalDistance = direction.length();
        direction.normalize();

        int linkCount = links.size();
        double spacing = totalDistance / (linkCount + 1);

        int index = 1;
        for (Map.Entry<Display, Double> entry : links.entrySet()) {
            Display link = entry.getKey();

            // Position this link along the chain
            Location linkPos = start.clone().add(direction.clone().multiply(spacing * index));
            linkPos.setDirection(direction);
            link.teleport(linkPos);

            index++;
        }

        // Position the lead at the target
        lead.teleport(end.setDirection(direction.clone().multiply(-1)));
    }

    protected abstract SoundEffect pullSound();

    protected abstract SoundEffect pushSound();

    protected abstract SoundEffect impactSound();

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        // Store locations for pull phase
        this.anchorLocation = caster == null ? location.clone() : caster.getLocation().clone();
        this.impactLocation = location.clone();

        final Entity hit = result.getHitEntity();
        impactSound().play(location);

        if (hit == null) {
            // PULL_CASTER can work without hitting an entity (wall grapple)
            if (pullMode != PullMode.PULL_CASTER) {
                setMarkForRemoval(true);
            }
            return;
        }

        this.hit = (LivingEntity) hit;
    }

    public void remove() {
        lead.remove();
        for (Display link : links.keySet()) {
            link.remove();
        }
    }
}