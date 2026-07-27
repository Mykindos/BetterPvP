package me.mykindos.betterpvp.core.item.impl.cannon.ride;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonBoardEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonLandEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonLaunchPlayerEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.PreCannonShootEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonConfig;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonDestination;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns every in-flight human cannonball: boarding, the camera, target selection, the ballistic arc, and returning the
 * rider to a normal game mode at the end - including when that end is a disconnect rather than a landing.
 * <p>
 * The rider's origin is written to {@link RideOriginStore} before anything visible happens, so every later step is
 * abortable by putting them back where they clicked.
 */
@Singleton
@CustomLog
public class CannonRideService {

    /** Downward acceleration per tick applied to the mannequin. Chosen to read as heavier than an arrow. */
    private static final double GRAVITY = 0.08;

    /** Relative-flag mask marking X, Y and Z relative so a look packet moves the camera not at all. */
    private static final byte LOOK_ONLY = 0x07;

    private final Core core;
    private final CannonConfig config;
    private final RideOriginStore originStore;
    private final Map<UUID, CannonRide> rides = new ConcurrentHashMap<>();

    @Inject
    private CannonRideService(Core core, CannonConfig config, RideOriginStore originStore) {
        this.core = core;
        this.config = config;
        this.originStore = originStore;
    }

    public @NotNull Optional<CannonRide> of(@NotNull UUID rider) {
        return Optional.ofNullable(rides.get(rider));
    }

    public boolean isRiding(@NotNull UUID rider) {
        return rides.containsKey(rider);
    }

    public @NotNull Collection<CannonRide> rides() {
        return rides.values();
    }

    /**
     * Puts {@code player} inside {@code cannon}: records where to put them back, swaps their body for a mannequin, and
     * moves their camera onto it.
     *
     * @return {@code true} if they boarded
     */
    public boolean board(@NotNull Player player, @NotNull CannonProp cannon) {
        if (rides.containsKey(player.getUniqueId())) {
            return false;
        }

        final CannonBoardEvent event = new CannonBoardEvent(cannon, player);
        event.callEvent();
        if (event.isCancelled()) {
            return false;
        }

        // Persist first. Everything after this point is recoverable precisely because this line already ran.
        final RideOrigin origin = RideOrigin.capture(player.getUniqueId(), player.getLocation(), player.getGameMode());
        originStore.put(origin);

        final Location muzzle = cannon.getMuzzle();
        muzzle.add(cannon.getEntity().getLocation().getDirection().multiply(-0.5));
        muzzle.subtract(0, 0.4, 0);
        final RideMannequin mannequin = new RideMannequin(player, muzzle);
        final CannonRide ride = new CannonRide(player.getUniqueId(), origin, mannequin, cannon);
        if (ride.isPrivateRide()) {
            mannequin.restrictTo(player.getUniqueId());
        }
        mannequin.show();
        rides.put(player.getUniqueId(), ride);

        player.setGameMode(GameMode.SPECTATOR);
        // Spectating a real entity locks movement and teleportation for free; the armour stand exists purely so there
        // is such an entity to spectate, and its own facing frames the shot.
        player.setSpectatorTarget(mannequin.getCamera());
        play(ride, player, new SoundEffect(Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1f, 0.8f), muzzle);
        return true;
    }

    /**
     * Lights this rider's own fuse, for a cannon that runs privately.
     * <p>
     * The countdown lives on the ride rather than on the cannon's cycle, which is what lets several people be mid-shot
     * in the same emplacement at once: the cannon itself is never claimed, so it never has to be handed back.
     */
    public void beginFuse(@NotNull UUID rider) {
        of(rider).ifPresent(ride -> ride.enter(RidePhase.FUSING));
    }

    /**
     * The cannon's fuse has burned out; offer the rider the cannon's destinations.
     * <p>
     * A cannon with no destinations declared has nothing to choose between, so it simply fires along its own barrel.
     *
     * @return {@code false} if there is no longer a rider to aim - the cannon must recover rather than wait for a shot
     * that will never be taken
     */
    public boolean beginTargeting(@NotNull UUID rider) {
        final CannonRide ride = rides.get(rider);
        if (ride == null) {
            return false;
        }

        ride.enter(RidePhase.TARGETING);
        final Player player = Bukkit.getPlayer(rider);
        if (player == null) {
            return false;
        }

        final CannonProp cannon = ride.getCannon();
        final List<CannonDestination> destinations = cannon == null ? List.of() : cannon.getDestinations();
        if (destinations.isEmpty()) {
            // Deferred a tick: this runs inside the cycle's fuse transition, and launching now would close the
            // shot before the cycle applies the state this call returns, leaving the cannon stuck in TARGETING.
            UtilServer.runTask(core, () -> launch(ride));
            return true;
        }

        new SoundEffect(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.6f).play(player);
        new CannonDestinationMenu(destinations, destination -> chooseDestination(rider, destination)).show(player);
        return true;
    }

    /** Locks in a destination and fires immediately. Ignored unless the rider is still choosing. */
    public void chooseDestination(@NotNull UUID rider, @NotNull CannonDestination destination) {
        of(rider).filter(ride -> ride.getPhase() == RidePhase.TARGETING).ifPresent(ride -> {
            ride.setTarget(destination.getLocation());
            launch(ride);
        });
    }

    /**
     * Solves for the velocity that lands the rider exactly on their chosen point and lets go. Flight time is picked
     * from the distance and the arc derived from it, so every shot lands where it was aimed and long shots hang in the
     * air longer.
     */
    private void launch(@NotNull CannonRide ride) {
        final Player player = Bukkit.getPlayer(ride.getRider());
        final Location from = ride.getMannequin().getLocation();
        final CannonProp cannon = ride.getCannon();
        if (cannon == null || !cannon.isMaterialized()) {
            abort(ride);
            return;
        }

        // No destination chosen (nothing declared, or the rider let the window lapse without picking): fall back to
        // firing along the barrel.
        final Location to = ride.getTarget() != null
                ? ride.getTarget()
                : from.clone().add(cannon.getEntity().getLocation().getDirection().multiply(cannon.getProperties().getPower()));

        final double distance = from.distance(to);
        final int ticks = (int) Math.clamp(distance / 1.2, config.getRideMinFlightTicks(), config.getRideMaxFlightTicks());
        final Vector velocity = new Vector(
                (to.getX() - from.getX()) / ticks,
                (to.getY() - from.getY()) / ticks + GRAVITY * (ticks - 1) / 2.0,
                (to.getZ() - from.getZ()) / ticks);

        ride.setVelocity(velocity);
        ride.enter(RidePhase.FLYING);

        // A private cannon was never claimed by this rider, so there is no cycle to close - and closing it would drop
        // the whole emplacement into a cooldown that everyone else sitting in it would have to wait out.
        if (!ride.isPrivateRide() && cannon.getCycle() != null) {
            cannon.getCycle().completeShot();
        }
        if (player != null) {
            new CannonLaunchPlayerEvent(cannon, player, to).callEvent();
        }

        // The rider is airborne from here on, and a body in the sky gives nothing away about the cannon it came from.
        ride.getMannequin().reveal();

        play(ride, player, new SoundEffect("littleroom_cannon", "littleroom.cannon.fire", 1f, 2f), from);
        spawn(ride, player, Particle.EXPLOSION_EMITTER.builder().location(from).extra(0));
    }

    /** Plays {@code effect} at {@code where} - across the area, or to the rider alone on a private ride. */
    private void play(@NotNull CannonRide ride, @Nullable Player player, @NotNull SoundEffect effect,
                      @NotNull Location where) {
        if (!ride.isPrivateRide()) {
            effect.play(where);
        } else if (player != null) {
            effect.play(player, where);
        }
    }

    /** Particle counterpart of {@link #play}. */
    private void spawn(@NotNull CannonRide ride, @Nullable Player player, @NotNull ParticleBuilder particle) {
        if (!ride.isPrivateRide()) {
            particle.receivers(60).spawn();
        } else if (player != null) {
            particle.receivers(player).spawn();
        }
    }

    /** Advances every live ride one tick. Called from {@code CannonRideListener}. */
    public void tick() {
        for (CannonRide ride : rides.values()) {
            final Player player = Bukkit.getPlayer(ride.getRider());
            if (player == null) {
                continue; // offline mid-flight; the origin record restores them on join
            }

            switch (ride.getPhase()) {
                case BOARDING -> holdCameraOnCannon(ride, player);
                case FUSING -> tickFusing(ride, player);
                case TARGETING -> tickTargeting(ride, player);
                case FLYING -> tickFlight(ride, player);
                case FINISHED -> {
                }
            }
        }
        rides.values().removeIf(ride -> ride.getPhase() == RidePhase.FINISHED);
    }

    /**
     * Holds the rider's view on the cannon while they sit in the barrel.
     * <p>
     * Both mechanisms are driven from the same target: the camera stand is pointed at the cannon, and the rider is
     * sent the identical rotation. Whether the client takes its facing from the spectated entity or from its own
     * input, it ends up looking at the same place, so the two can never fight each other.
     *
     * @return the cannon they are aboard, or {@code null} if it has gone and the ride was aborted
     */
    private @Nullable CannonProp holdCameraOnCannon(@NotNull CannonRide ride, @NotNull Player player) {
        final CannonProp cannon = ride.getCannon();
        if (cannon == null || !cannon.isMaterialized()) {
            abort(ride);
            return null;
        }
        final Location camera = ride.getMannequin().getCamera().getLocation();
        final Vector toCannon = cannon.getLocation().toVector().add(new Vector(0, 2.5, 0)).subtract(camera.toVector());
        if (toCannon.lengthSquared() > 1.0E-4) {
            ride.getMannequin().aimCamera(toCannon);
            lookAt(player, toCannon.add(new Vector(0, 1.5, 0)));
        }
        return cannon;
    }

    /**
     * Burns this rider's own fuse on a private cannon, then hands them over to targeting.
     * <p>
     * The crackle, the sparks and the countdown all go to the rider alone, so the several people who may be sitting in
     * the same barrel at the same time each see only their own shot coming.
     */
    private void tickFusing(@NotNull CannonRide ride, @NotNull Player player) {
        final CannonProp cannon = holdCameraOnCannon(ride, player);
        if (cannon == null) {
            return;
        }

        final long remaining = ride.getFuseMillis() - ride.millisInPhase();
        if (remaining <= 0) {
            // The cannon's own cycle raises this on a shared emplacement; a private ride is its own cycle and must
            // still give anything gating cannon fire its say. A veto has no chambered state to fall back on, so the
            // rider simply gets put back where they climbed in.
            final PreCannonShootEvent event = new PreCannonShootEvent(cannon, player, ride.getRider());
            event.callEvent();
            if (event.isCancelled()) {
                abort(ride);
                return;
            }

            beginTargeting(ride.getRider());
            return;
        }

        cannon.emitFuse(player);
    }

    /**
     * Holds the camera steady on the cannon while the destination menu is open, and fires anyway if the rider never
     * picks - an abandoned menu must not leave them stuck in the barrel.
     */
    private void tickTargeting(@NotNull CannonRide ride, @NotNull Player player) {
        final CannonProp cannon = ride.getCannon();
        if (cannon != null && cannon.isMaterialized()) {
            final Location camera = ride.getMannequin().getCamera().getLocation();
            final Vector toCannon = cannon.getLocation().toVector().add(new Vector(0, 2.5, 0)).subtract(camera.toVector());
            if (toCannon.lengthSquared() > 1.0E-4) {
                ride.getMannequin().aimCamera(toCannon);
                lookAt(player, toCannon);
            }
        }

        if (ride.millisInPhase() >= (long) (config.getRideTargetingSeconds() * 1000L)) {
            final List<CannonDestination> destinations = cannon == null ? List.of() : cannon.getDestinations();
            if (!destinations.isEmpty()) {
                ride.setTarget(destinations.getFirst().getLocation());
            }
            player.closeInventory();
            launch(ride);
        }
    }

    private void tickFlight(@NotNull CannonRide ride, @NotNull Player player) {
        final Vector velocity = ride.getVelocity();
        final Location origin = ride.getMannequin().getLocation();
        final Location next = ride.getMannequin().getLocation().add(velocity);
        velocity.setY(velocity.getY() - GRAVITY);

        ride.getMannequin().moveTo(next, velocity);
        // Keep the view on the mannequin for the whole flight, so the rider watches themselves fly rather than
        // whatever they happened to be facing at launch. moveTo already turned the camera stand to match.
        final Location camera = ride.getMannequin().getCamera().getLocation();
        final Vector toBody = next.toVector().subtract(camera.toVector());
        if (toBody.lengthSquared() > 1.0E-4) {
            lookAt(player, toBody);
        }
        spawn(ride, player, Particle.CLOUD.builder().location(origin).extra(0).count(2).offset(0.1, 0.1, 0.1));

        final boolean grounded = !next.getBlock().isPassable()
                || !next.clone().subtract(0, 0.2, 0).getBlock().isPassable();
        if (grounded || next.getY() < next.getWorld().getMinHeight()) {
            land(ride, player, next);
        }
    }

    /** Drops the rider back into the world where their stand-in came down. */
    private void land(@NotNull CannonRide ride, @NotNull Player player, @NotNull Location at) {
        final Location safe = at.clone().add(0, 0.05, 0);
        safe.setYaw(player.getLocation().getYaw());
        safe.setPitch(0f);

        release(player, ride);
        player.teleport(safe);
        player.setGameMode(ride.getOrigin().toGameMode());

        new SoundEffect(Sound.ENTITY_PLAYER_BIG_FALL, 1f, 1f).play(safe);
        Particle.EXPLOSION.builder().location(safe).extra(0).count(1).receivers(60).spawn();
        new CannonLandEvent(ride.getCannon(), player, safe).callEvent();
    }

    /**
     * Ends a ride without a landing (the cannon vanished, the rider was ejected). The rider goes back to the exact
     * spot they clicked from.
     */
    public void abort(@NotNull CannonRide ride) {
        final Player player = Bukkit.getPlayer(ride.getRider());
        if (player == null) {
            ride.enter(RidePhase.FINISHED);
            ride.getMannequin().remove();
            return;
        }

        final Location origin = ride.getOrigin().toLocation();
        release(player, ride);
        if (origin != null) {
            player.teleport(origin);
        }
        player.setGameMode(ride.getOrigin().toGameMode());
    }

    /**
     * Tears down the camera and mannequin and drops the durable origin record. The ride is marked finished first so
     * the un-spectate guard does not block this release.
     */
    private void release(@NotNull Player player, @NotNull CannonRide ride) {
        ride.enter(RidePhase.FINISHED);
        player.setSpectatorTarget(null);
        ride.getMannequin().remove();
        originStore.remove(ride.getRider());
    }

    /**
     * Restores a player who logged out or crashed mid-ride. Runs on join for anyone holding an origin record, whether
     * or not this server process knew about their flight.
     */
    public void restoreIfStranded(@NotNull Player player) {
        final RideOrigin origin = originStore.get(player.getUniqueId()).orElse(null);
        if (origin == null) {
            return;
        }

        final CannonRide ride = rides.remove(player.getUniqueId());
        if (ride != null) {
            ride.getMannequin().remove();
        }

        final Location location = origin.toLocation();
        // Deferred a tick: teleporting and changing game mode inside the join event itself is unreliable.
        UtilServer.runTaskLater(core, () -> {
            player.setSpectatorTarget(null);
            if (location != null) {
                player.teleport(location);
            }
            player.setGameMode(origin.toGameMode());
            originStore.remove(player.getUniqueId());
            log.info("Restored {} to their cannon boarding position after a mid-ride disconnect", player.getName()).submit();
        }, 2L);
    }

    /** Leaves the durable record in place so the rider is restored on their next join. */
    public void onQuit(@NotNull UUID rider) {
        final CannonRide ride = rides.remove(rider);
        if (ride != null) {
            ride.getMannequin().remove();
        }
    }

    /** Drops every rider still aboard a cannon that is going away. */
    public void evictFrom(@NotNull CannonProp cannon) {
        for (CannonRide ride : rides.values()) {
            if (ride.getCannon() == cannon && ride.getPhase() != RidePhase.FLYING) {
                abort(ride);
            } else if (ride.getCannon() == cannon) {
                ride.setCannon(null); // already airborne - let the flight finish on its own
            }
        }
    }

    /** Turns the rider's head to face along {@code direction}. */
    private void lookAt(@NotNull Player player, @NotNull Vector direction) {
        final Location facing = player.getLocation();
        facing.setDirection(direction);
        player.setRotation(facing.getYaw(), facing.getPitch());
    }
}
