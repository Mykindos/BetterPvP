package me.mykindos.betterpvp.core.item.impl.cannon.ride;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The rider's stand-in while they are in the air: a mannequin wearing the rider's own skin, frozen in the swimming
 * pose, plus a real invisible armour stand trailing a few blocks behind it that the rider spectates.
 * <p>
 * A mannequin renders a player skin and holds a fixed pose without a second logged-in player, so the visible body is an
 * ordinary entity. The camera is separate because {@code Player#setSpectatorTarget} frames the shot from the spectated
 * entity's own facing - pointing a stand at the mannequin is what gives the third-person view over its shoulder. Both
 * move together each tick; the client interpolates entity motion, so the camera glides rather than steps.
 */
public class RideMannequin {

    private final @NotNull Plugin plugin;
    private final @NotNull Mannequin body;
    private final @NotNull Location location;

    /** The real entity the rider spectates. Invisible and marker-sized so it never renders, collides, or is hit. */
    @Getter private final @NotNull ArmorStand camera;

    /** While set, the only client the body is shown to. Dropped by {@link #reveal()} once the shot is away. */
    private @Nullable UUID exclusiveViewer;

    public RideMannequin(@NotNull Plugin plugin, @NotNull Player rider, @NotNull Location start) {
        this.plugin = plugin;
        this.location = start.clone();

        this.body = start.getWorld().spawn(this.location, Mannequin.class, mannequin -> {
            mannequin.setProfile(ResolvableProfile.resolvableProfile(rider.getPlayerProfile()));
            mannequin.setDescription(null);
            mannequin.setCustomNameVisible(false);
            mannequin.setImmovable(true);
            mannequin.setInvulnerable(true);
            mannequin.setGravity(false);
            mannequin.setSilent(true);
            mannequin.setPersistent(false);
            mannequin.setCollidable(false);
            mannequin.setAI(false);
            mannequin.setPose(Pose.SWIMMING, true);
        });

        this.camera = start.getWorld().spawn(cameraSpot(start, start.getDirection()), ArmorStand.class, stand -> {
            stand.setMarker(true);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
            stand.setPersistent(false);
            stand.setCollidable(false);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setSilent(true);
        });
    }

    /** Current world position of the visible body. */
    public @NotNull Location getLocation() {
        return location.clone();
    }

    /**
     * Restricts the body to a single client, for a cannon that runs its cycle privately. The rider sits in the barrel
     * as their own stand-in without anyone else seeing a body appear there.
     */
    public void restrictTo(@NotNull UUID viewer) {
        this.exclusiveViewer = viewer;
    }

    /** Opens the body up to everyone, once it is airborne and no longer gives away a cannon that looks idle. */
    public void reveal() {
        this.exclusiveViewer = null;
        show();
    }

    /** Brings every online client's view of the body in line with who is entitled to see it. */
    public void show() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            applyVisibility(viewer);
        }
    }

    /**
     * Shows or hides the body for one client. The server tracks the mannequin for everyone by default, so restricting
     * a ride means hiding the body from all but its rider rather than sending it only to them.
     */
    public void applyVisibility(@NotNull Player viewer) {
        if (exclusiveViewer == null || exclusiveViewer.equals(viewer.getUniqueId())) {
            viewer.showEntity(plugin, body);
        } else {
            viewer.hideEntity(plugin, body);
        }
    }

    /** Moves the body to {@code to} and trails the camera behind it. */
    public void moveTo(@NotNull Location to, @NotNull Vector velocity) {
        location.setX(to.getX());
        location.setY(to.getY());
        location.setZ(to.getZ());
        if (velocity.lengthSquared() > 1.0E-4) {
            final Location facing = location.clone();
            facing.setDirection(velocity);
            location.setYaw(facing.getYaw());
            location.setPitch(facing.getPitch());
        }

        body.teleport(location);
        camera.teleport(cameraSpot(location, location.getDirection()));
    }

    /**
     * Places the camera behind and above the body along {@code behind}, facing the body.
     * <p>
     * The lift is taken perpendicular to {@code behind} rather than along world up, so the boom pitches with the arc -
     * below the body on a climb, above it on a dive - while staying a constant distance from it. A world-space lift
     * would instead shorten the boom on the way up and stretch it on the way down.
     * <p>
     * The boom's pitch is capped because past {@code atan(back / up)} - about 66 degrees for these lengths - the arm
     * swings over the top of the body and comes down in front of it, turning the shot around to face backwards. Long
     * flights reach that angle late in the dive, since vertical speed keeps growing while horizontal speed is fixed.
     * <p>
     * The stand's own rotation matters: a client spectating an entity renders from that entity's facing, so pointing
     * the stand at the mannequin is what frames the shot.
     */
    private static @NotNull Location cameraSpot(@NotNull Location body, @NotNull Vector behind) {
        final Location boom = body.clone();
        boom.setDirection(behind);
        if (behind.getX() * behind.getX() + behind.getZ() * behind.getZ() < 1.0E-4) {
            // Dead vertical: the direction carries no bearing for the boom to sit behind, but the body's yaw still
            // remembers the one it had before the arc went vertical.
            boom.setYaw(body.getYaw());
        }
        boom.setPitch(Math.clamp(boom.getPitch(), -55f, 25f));

        final Vector d = boom.getDirection();
        final Vector localUp = new Vector(0, 1, 0).subtract(d.clone().multiply(d.getY())).normalize();
        final Vector offset = d.multiply(-4.0).add(localUp.multiply(1.8));
        final Location spot = body.clone().add(offset);
        spot.setDirection(body.toVector().subtract(spot.toVector()));
        return spot;
    }

    /**
     * Points the camera along {@code direction} from its current position, for the aiming window where the rider
     * steers the view themselves.
     */
    public void aimCamera(@NotNull Vector direction) {
        final Location spot = camera.getLocation();
        spot.setDirection(direction);
        camera.setRotation(spot.getYaw(), spot.getPitch());
    }

    /** Despawns both the body and the camera. */
    public void remove() {
        if (body.isValid()) {
            body.remove();
        }
        if (camera.isValid()) {
            camera.remove();
        }
    }
}
