package me.mykindos.betterpvp.core.cutscene.camera;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The invisible marker a viewer's camera is welded to for the length of a cutscene.
 * <p>
 * Nothing more than a point in the world: an {@link ItemDisplay} that is never given an item, so it renders nothing,
 * carries no offset from the shot, and has no relationship to the view beyond being where it is. The camera is the
 * marker, not something arranged around it.
 * <p>
 * Deliberately tracked for everyone rather than hidden and shown to its viewer. It draws nothing, so hiding it buys
 * nothing - and per-viewer visibility puts the spawn packet on a path that can arrive after the order to spectate it,
 * which is precisely how the camera ends up pointing at nowhere.
 * <p>
 * A display is used rather than a re-teleported player because it can be moved every tick without fighting the client
 * for control of the view: spectating a real entity hands the client that entity's position and facing outright, and
 * locks movement and teleportation as a side effect. That is why the gating list in the design is so short - most of
 * it comes free with the game mode.
 * <p>
 * <b>Interpolation is deliberately one tick, not the length of the travel.</b> A display's teleport duration
 * interpolates <em>linearly</em>, so handing it the whole travel would fix every transition at constant speed and make
 * {@link Easing} unexpressible. At one tick it only smooths the seam between server ticks while
 * {@link CameraDriver} owns the curve.
 *
 * @see CameraDriver
 */
public class CameraMarker {

    /** Smooths the gap between consecutive server ticks without taking over the easing curve. */
    private static final int INTERPOLATION_TICKS = 1;

    private final ItemDisplay camera;
    private boolean released;

    public CameraMarker(@NotNull Location start) {
        final World world = start.getWorld();
        this.camera = world.spawn(start, ItemDisplay.class, display -> {
            display.setPersistent(false);
            display.setGravity(false);
            display.setSilent(true);
            display.setInvulnerable(true);
            display.setTeleportDuration(INTERPOLATION_TICKS);
        });
    }

    public @NotNull ItemDisplay getEntity() {
        return camera;
    }

    /**
     * Puts {@code viewer} on the marker.
     * <p>
     * Must not run on the same tick the marker was spawned. The entity tracker flushes spawn packets at the end of a
     * tick, so a client told to spectate an entity it has not been sent yet has nothing to position the camera on: it
     * keeps rendering from where the player was standing, and no chunks stream in around where the camera actually is.
     * That is the same desync that bites packet-based camera rigs, and the fix is the same - let the spawn land first.
     */
    public void attach(@NotNull Player viewer) {
        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.setSpectatorTarget(camera);
    }

    /** Where the marker is now - what the viewer's own body is kept near so the world streams around the shot. */
    public @NotNull Location getLocation() {
        return camera.getLocation();
    }

    /** Re-asserts the spectator target, for the tick after something tried to clear it. */
    public void reattach(@NotNull Player viewer) {
        if (released || !camera.isValid()) {
            return;
        }
        if (viewer.getGameMode() != GameMode.SPECTATOR) {
            viewer.setGameMode(GameMode.SPECTATOR);
        }
        if (viewer.getSpectatorTarget() != camera) {
            viewer.setSpectatorTarget(camera);
        }
    }

    /** Moves the camera, letting the client interpolate across the single tick to get there. */
    public void apply(@NotNull CameraPose pose) {
        if (released || !camera.isValid()) {
            return;
        }
        camera.teleport(pose.toLocation(camera.getWorld()));
    }

    /**
     * Moves the camera with no interpolation at all - the hard cut. The duration is restored immediately, so only the
     * one move it brackets is a jump.
     */
    public void snap(@NotNull CameraPose pose) {
        if (released || !camera.isValid()) {
            return;
        }
        camera.setTeleportDuration(0);
        camera.teleport(pose.toLocation(camera.getWorld()));
        camera.setTeleportDuration(INTERPOLATION_TICKS);
    }

    /** Detaches the viewer without removing the camera, so the game mode can be restored before the entity goes. */
    public void release(@NotNull Player viewer) {
        released = true;
        if (viewer.getSpectatorTarget() == camera) {
            viewer.setSpectatorTarget(null);
        }
    }

    public void remove() {
        released = true;
        if (camera.isValid()) {
            camera.remove();
        }
    }
}
