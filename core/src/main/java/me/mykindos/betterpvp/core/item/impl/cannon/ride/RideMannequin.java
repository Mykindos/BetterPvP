package me.mykindos.betterpvp.core.item.impl.cannon.ride;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import lombok.Getter;
import me.mykindos.betterpvp.core.scene.npc.HumanNMS;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The rider's stand-in while they are in the air: a packet-only player entity wearing the rider's own skin, frozen in
 * the swimming pose, plus a real invisible armour stand trailing a few blocks behind it that the rider spectates.
 * <p>
 * Only a player entity renders a player skin and a swimming pose, and a packet entity is the only way to have one
 * without a second logged-in player - but {@code Player#setSpectatorTarget} needs a real entity the viewer's client
 * tracks, which a packet entity is not. So the visible body is packets and the camera is a real invisible armour stand
 * behind it, giving a third-person view over the mannequin's shoulder. Both move together each tick; the client
 * interpolates entity motion, so the camera glides rather than steps.
 */
public class RideMannequin {

    private static final double CAMERA_BACK = 4.0;
    private static final double CAMERA_UP = 1.6;

    private final @NotNull HumanNMS handle;
    private final @NotNull Location location;

    /** The real entity the rider spectates. Invisible and marker-sized so it never renders, collides, or is hit. */
    @Getter private final @NotNull ArmorStand camera;

    private final @NotNull String skinName;
    private final @Nullable String skinValue;
    private final @Nullable String skinSignature;

    /** Clients that have been sent this mannequin, so late joiners can be caught up mid-flight. */
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    /** While set, the only client the body is sent to. Dropped by {@link #reveal()} once the shot is away. */
    private @Nullable UUID exclusiveViewer;

    public RideMannequin(@NotNull Player rider, @NotNull Location start) {
        this.location = start.clone();
        this.skinName = rider.getName();

        final Optional<ProfileProperty> textures = rider.getPlayerProfile().getProperties().stream()
                .filter(property -> property.getName().equals("textures"))
                .findFirst();
        this.skinValue = textures.map(ProfileProperty::getValue).orElse(null);
        this.skinSignature = textures.map(ProfileProperty::getSignature).orElse(null);

        this.handle = new HumanNMS(rider.getName(), this.location, skinValue, skinSignature);
        this.handle.place();

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

    /** Spawns the body on every client entitled to see it and freezes it in the swimming pose. */
    public void show() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (canSee(viewer) && !viewers.contains(viewer.getUniqueId())) {
                showTo(viewer);
            }
        }
    }

    private boolean canSee(@NotNull Player viewer) {
        return viewer.getWorld().equals(location.getWorld())
                && (exclusiveViewer == null || exclusiveViewer.equals(viewer.getUniqueId()));
    }

    private void showTo(@NotNull Player viewer) {
        if (!viewer.getWorld().equals(location.getWorld())) {
            return;
        }

        final Entity entity = handle.getBukkitEntity();
        final UserProfile profile = new UserProfile(entity.getUniqueId(), skinName);
        if (skinValue != null && !skinValue.isBlank()) {
            profile.getTextureProperties().add(new TextureProperty("textures", skinValue, skinSignature));
        }

        // The profile entry must arrive before the spawn packet or the client renders a default skin. It is added
        // unlisted so the mannequin never appears in the tab list.
        final WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                profile, false, 0, GameMode.SURVIVAL, null, null);
        send(viewer, new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_LISTED), info));

        // Player entities always render their profile name overhead; a team with name-tag visibility NEVER is the
        // only way to suppress it.
        final WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(), Component.empty(), Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                WrapperPlayServerTeams.CollisionRule.NEVER,
                NamedTextColor.WHITE, WrapperPlayServerTeams.OptionData.NONE);
        send(viewer, new WrapperPlayServerTeams(teamName(), WrapperPlayServerTeams.TeamMode.CREATE,
                Optional.of(teamInfo), List.of(skinName)));

        send(viewer, new WrapperPlayServerSpawnEntity(entity.getEntityId(), Optional.of(entity.getUniqueId()),
                EntityTypes.PLAYER, new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(), location.getYaw(), location.getYaw(), 0, Optional.of(new Vector3d(0, 0, 0))));
        sendPose(viewer);
        viewers.add(viewer.getUniqueId());
    }

    /** Re-sends the metadata that keeps the body in the swimming pose with all skin layers enabled. */
    private void sendPose(@NotNull Player viewer) {
        send(viewer, new WrapperPlayServerEntityMetadata(handle.getBukkitEntity().getEntityId(), List.of(
                new EntityData<>(6, EntityDataTypes.ENTITY_POSE, EntityPose.SWIMMING),
                new EntityData<>(16, EntityDataTypes.BYTE, (byte) 0x7F))));
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
        handle.place();

        final int entityId = handle.getBukkitEntity().getEntityId();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!canSee(viewer)) {
                continue;
            }
            if (!viewers.contains(viewer.getUniqueId())) {
                showTo(viewer); // joined or changed world after launch
            }
            send(viewer, new WrapperPlayServerEntityTeleport(entityId,
                    new Vector3d(location.getX(), location.getY(), location.getZ()),
                    location.getYaw(), location.getPitch(), false));
            send(viewer, new WrapperPlayServerEntityHeadLook(entityId, location.getYaw()));
        }

        camera.teleport(cameraSpot(location, location.getDirection()));
    }

    /**
     * Places the camera behind and above the body along {@code behind}, facing the body.
     * <p>
     * The lift is taken perpendicular to {@code behind} rather than along world up, so the boom pitches with the arc -
     * below the body on a climb, above it on a dive - while staying a constant {@code hypot(CAMERA_BACK, CAMERA_UP)}
     * from it. A world-space lift would instead shorten the boom on the way up and stretch it on the way down.
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
        boom.setPitch(Math.clamp(boom.getPitch(), -55f, 45f));

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
        final Entity entity = handle.getBukkitEntity();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            send(viewer, new WrapperPlayServerDestroyEntities(entity.getEntityId()));
            send(viewer, new WrapperPlayServerPlayerInfoRemove(List.of(entity.getUniqueId())));
            send(viewer, new WrapperPlayServerTeams(teamName(), WrapperPlayServerTeams.TeamMode.REMOVE,
                    Optional.empty(), List.of()));
        }
        viewers.clear();
        if (camera.isValid()) {
            camera.remove();
        }
    }

    private @NotNull String teamName() {
        return "cannonride" + handle.getBukkitEntity().getEntityId();
    }

    private static void send(@NotNull Player viewer, @NotNull PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(packet);
    }
}
