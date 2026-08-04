package me.mykindos.betterpvp.clans.world.discovery;

import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.framework.shader.ScreenEffect;
import me.mykindos.betterpvp.core.framework.shader.ScreenEffectService;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Everything the crew sees and hears while the ship is turning, for one expedition.
 * <p>
 * All of it reads from {@link ShipDynamics#getRudder()}: the sign picks the side being steered toward and the magnitude
 * scales how hard the ship is complaining about it. Nothing is broadcast — the whole show is scoped to the people
 * aboard, because a limbo world's only inhabitants are its crew and a world-wide particle burst is a thousand packets
 * sent to nobody.
 * <p>
 * The counters here are why this is an object per expedition rather than a static helper: a splash every tick at a
 * thousand particles a time is an unreasonable amount of work, and the irregular gaps are what stop the wake and the
 * hull from sounding like a metronome.
 */
public class SteeringCues {

    /** Below this the wheel is centred enough that the ship is not visibly turning. */
    private static final double DEAD_ZONE = 0.05;

    /** How far the idle swell tips the horizon, as a fraction of a hard turn's heel. */
    private static final double SWELL_HEEL = 0.07;

    /** Radians of the slow swell per tick — a lift and settle roughly every seven seconds. */
    private static final double SWELL_SLOW = 2 * Math.PI / 140.0;

    /** The faster chop laid over it, on a period that shares no whole multiple with the slow one. */
    private static final double SWELL_FAST = 2 * Math.PI / 83.0;

    /** Sideways nudge per tick at the top of a swell, well under the shove a hard turn gives. */
    private static final double SWELL_PUSH = 0.004;

    /** Sideways nudge per tick at full rudder. */
    private static final double TURN_PUSH = 0.02;

    /** Ticks between drift packets; the nudge is multiplied to match, so the drift itself is unaffected. */
    private static final int DRIFT_INTERVAL = 4;

    private final ScreenEffectService screenEffects;

    /** Timbers taking the sea, the loudest of the three and the one that carries the ship's motion. */
    private final AmbientVoice hullKnock = new AmbientVoice(Sound.BLOCK_BAMBOO_WOOD_HIT, 0.0f, 0.5f, 0.6f, 3, 20, 70);

    /** Water shouldering past the hull. */
    private final AmbientVoice waterRush = new AmbientVoice(Sound.ENTITY_BOAT_PADDLE_WATER, 0.0f, 0.3f, 0.5f, 2, 30, 90);

    /** Seabirds, standing in for a call the game does not have. Sparse, so they read as distant rather than as a flock. */
    private final AmbientVoice seabirds = new AmbientVoice(Sound.ENTITY_DOLPHIN_PLAY, 0.0f, 2.0f, 0.35f, 3, 120, 400);

    private int wakeTick;
    private int wakeSoundIn;
    private int strainSoundIn;
    private int creakSoundIn;
    private int helmSoundIn;
    private int arrowFlash;

    /** The swell's own clock, in ticks. */
    private int swellTick;

    /** The previous wheel position, so a wheel coming back off the stops can be told from one going onto them. */
    private double lastRudder;

    /** Whether the correcting clip has already been asked for on this return, so it fires once and not every tick. */
    private boolean correcting;

    public SteeringCues(@NotNull ScreenEffectService screenEffects) {
        this.screenEffects = screenEffects;
    }

    public void emit(@NotNull Expedition expedition, @NotNull SteeringControls controls, @Nullable Location helm,
                     @Nullable ActiveModel helmModel, @NotNull List<Player> crew, long now) {
        final double rudder = expedition.getDynamics().getRudder();
        final double swell = swell(rudder);

        arrows(controls, now);

        if (!crew.isEmpty()) {
            wake(expedition, rudder, crew);
            hull(expedition, rudder, crew);
            helm(controls, helm, rudder, crew, now);
            ambience(crew);

            heel(rudder, swell, crew);
            drift(expedition, rudder * TURN_PUSH + swell * SWELL_PUSH, crew);
        }

        animateHelm(helmModel, rudder);
        this.lastRudder = rudder;
    }

    /**
     * Green while a control is actually being held, and a slow yellow-white blink otherwise so an idle control still
     * reads as something to grab rather than as scenery.
     */
    private void arrows(@NotNull SteeringControls controls, long now) {
        arrowFlash++;
        final Color idle = (arrowFlash / 8) % 2 == 0 ? Color.YELLOW : Color.WHITE;

        for (SteerSide side : SteerSide.values()) {
            controls.control(side).tintArrow(controls.getInput().isActive(side, now) ? Color.LIME : idle);
        }
    }

    /**
     * The sea working under a hull nobody is steering, as a signed number in {@code [-1, 1]}.
     * <p>
     * Two sines rather than one, on periods with no common multiple, so the ship never repeats the same lean twice in a
     * row and the motion reads as water rather than as an animation loop. It fades out as the wheel comes off centre:
     * a crew hauling the ship round should feel the turn, not the turn plus a swell fighting it.
     */
    private double swell(double rudder) {
        swellTick++;
        final double raw = Math.sin(swellTick * SWELL_SLOW) * 0.7 + Math.sin(swellTick * SWELL_FAST + 1.3) * 0.3;
        return raw * (1.0 - Math.min(1.0, Math.abs(rudder)));
    }

    /**
     * The deck leaning under the turn, drawn client-side by the resource pack's core shaders.
     * <p>
     * Sign is away from the turn, matching the shove the crew already gets: a ship hauled hard to starboard heels to
     * port, so the horizon swings the other way from the wheel. Pushed every tick rather than only off centre, because
     * the shader keeps nothing between frames — coming back to level has to be driven, and a crew that stops steering
     * mid-heel would otherwise stay tilted. That same every-tick push is what lets the idle swell ride on top of it: with a
     * centred wheel the whole lean is the swell, and level is only ever a value the swell happens to be passing through.
     */
    private void heel(double rudder, double swell, @NotNull List<Player> crew) {
        screenEffects.drive(crew, ScreenEffect.ROLL, rudder + swell * SWELL_HEEL);
    }

    /**
     * The deck sliding out from under the crew — from the turn, from the swell, or from both.
     * <p>
     * Nudged rather than set, so it builds over a second or so against the player's own drag instead of shoving them.
     * Sign is away from the turn: a ship going hard to starboard throws you to port.
     * <p>
     * Throttled, and not to save packets: every packet sent is a chance of landing on the start of somebody's jump a
     * round trip later. Four times fewer sends is four times fewer spoiled jumps, and the drift itself is unchanged
     * because the force per send goes up to match. That matters more now than it did when only a turn moved anyone —
     * the swell never stops, so without the throttle a crew standing at a centred wheel would be taking a velocity
     * packet every tick for the whole voyage.
     *
     * @param lateral how hard to nudge, along the moored hull's beam
     */
    private void drift(@NotNull Expedition expedition, double lateral, @NotNull List<Player> crew) {
        if (swellTick % DRIFT_INTERVAL != 0 || Math.abs(lateral) < 0.0005) {
            return;
        }

        final double push = lateral * DRIFT_INTERVAL;
        final ShipFrame frame = expedition.getFrame();
        for (Player sailor : crew) {
            final ServerPlayer handle = ((CraftPlayer) sailor).getHandle();

            // The client treats a velocity packet as a replacement, not an addition, so what it really sends is
            // the server's guess at this player's motion. It is only safe to push while that guess is right,
            // which is a narrower window than it looks:
            //   isOnGround   - covers the arc, but is a tick stale, so it is still true on the launch tick
            //   isJumping    - covers that launch tick, when the server has not seen the climb yet
            //   vertical > 0 - covers a rise the ground flag has already lost track of
            //   vertical low - covers the landing tick, where the server still holds the impact speed
            //   xxa / zza    - covers walking, where a packet stamps out their stride
            final double vertical = handle.getDeltaMovement().y;
            // These conditions keep the packet off anyone the server cannot describe accurately. They cannot
            // catch the case that actually hurts: a packet sent on the last grounded tick arrives a round trip
            // later, which is sometimes the exact moment a jump starts, and it overwrites the launch. At send
            // time there is no jump to see. That is latency, not a missing check, and it is why the drift stays
            // gentle — carrying a player properly means a vehicle they stand on, not velocity packets.
            if (!sailor.isOnGround() || sailor.isJumping()
                    || vertical > 0.0 || vertical < -0.2
                    || handle.xxa != 0f || handle.zza != 0f) {
                continue;
            }

            handle.push(frame.rightX() * push, 0, frame.rightZ() * push);
            handle.hurtMarked = true;
        }
    }

    /** The ship simply being a ship: timbers, water and birds, wheel or no wheel. */
    private void ambience(@NotNull List<Player> crew) {
        hullKnock.tick(crew);
        waterRush.tick(crew);
        seabirds.tick(crew);
    }

    /**
     * Spray down the side the ship is turning toward, covering the whole length of that side.
     * <p>
     * Which world axis the hull runs along depends on how the vessel was moored, so it comes off the hull's own bounds
     * and the anchor's facing rather than being assumed.
     */
    private void wake(@NotNull Expedition expedition, double rudder, @NotNull List<Player> crew) {
        if (Math.abs(rudder) <= DEAD_ZONE) {
            return;
        }

        if (--wakeSoundIn <= 0) {
            wakeSoundIn = UtilMath.randomInt(1, 4);
            final SoundEffect splash = new SoundEffect(Sound.ENTITY_GENERIC_SPLASH,
                    (float) UtilMath.randDouble(0.0, 0.5), 1.4f);
            crew.forEach(splash::play);
        }

        // A thousand particles at twenty hertz per player is more than the effect is worth.
        if (++wakeTick % 2 != 0) {
            return;
        }

        final BoundingBox hull = expedition.getHull();
        final World world = expedition.getAnchor().getWorld();
        if (world == null) {
            return;
        }

        final boolean lengthOnX = hull.getWidthX() >= hull.getWidthZ();
        final ShipFrame frame = expedition.getFrame();
        final double starboardSign = Math.signum(lengthOnX ? frame.rightZ() : frame.rightX());
        final double toward = (starboardSign == 0 ? 1 : starboardSign) * Math.signum(rudder);

        final double halfBeam = 5.0;
        final Location origin = new Location(world,
                hull.getCenterX() + (lengthOnX ? 0.0 : toward * halfBeam),
                hull.getMinY() + 5,
                hull.getCenterZ() + (lengthOnX ? toward * halfBeam : 0.0));

        Particle.CLOUD.builder()
                .location(origin)
                .count(50)
                // A third of the hull's height, so the spray breaks against the side rather than climbing the mast.
                .offset(lengthOnX ? hull.getWidthX() / 12.0 : 1.0,
                        2.5,
                        lengthOnX ? 1.0 : hull.getWidthZ() / 12.0)
                .extra(0)
                .receivers(crew)
                .spawn();
    }

    /** The vessel itself working against the turn, loudest when the wheel is near the stops. */
    private void hull(@NotNull Expedition expedition, double rudder, @NotNull List<Player> crew) {
        final World world = expedition.getAnchor().getWorld();
        if (world == null) {
            return;
        }

        final double force = Math.abs(rudder);
        final Location centre = expedition.getHull().getCenter().toLocation(world);

        if (force > 0.6 && --strainSoundIn <= 0) {
            strainSoundIn = UtilMath.randomInt(3, 9);
            final SoundEffect strain = new SoundEffect(Sound.BLOCK_CHEST_CLOSE, (float) UtilMath.randDouble(0.0, 0.5), 4f);
            crew.forEach(sailor -> strain.play(sailor, centre));
        }

        if (force > DEAD_ZONE && force <= 0.6 && --creakSoundIn <= 0) {
            creakSoundIn = UtilMath.randomInt(4, 12);
            final SoundEffect creak = new SoundEffect(Sound.BLOCK_BAMBOO_WOOD_DOOR_OPEN,
                    (float) UtilMath.randDouble(0.4, 0.8), 4f);
            crew.forEach(sailor -> creak.play(sailor, centre));
        }
    }

    /** The wheel itself clicking round, rising in pitch as it comes off centre. Only while somebody is turning it. */
    private void helm(@NotNull SteeringControls controls, @Nullable Location helm, double rudder,
                      @NotNull List<Player> crew, long now) {
        if (helm == null || !anyHeld(controls, now)) {
            return;
        }
        if (--helmSoundIn > 0) {
            return;
        }

        helmSoundIn = 3;
        final SoundEffect click = new SoundEffect(Sound.BLOCK_BAMBOO_WOOD_DOOR_CLOSE,
                (float) (0.6 + Math.abs(rudder) * 0.8), 0.5f);
        crew.forEach(sailor -> click.play(sailor, helm));
    }

    private static boolean anyHeld(@NotNull SteeringControls controls, long now) {
        return controls.getInput().isActive(SteerSide.PORT, now)
                || controls.getInput().isActive(SteerSide.STARBOARD, now);
    }

    /**
     * The wheel's own animation: spinning while it is being wound onto the stops, and a single correcting turn when it
     * starts coming back.
     * <p>
     * Re-requesting a clip every tick is free because {@code force = false} lets ModelEngine drop a request for
     * something already playing, so there is no need to track what the wheel is in the middle of.
     */
    private void animateHelm(@Nullable ActiveModel model, double rudder) {
        if (model == null) {
            return;
        }

        final boolean returning = Math.abs(rudder) < Math.abs(lastRudder);
        if (!returning) {
            correcting = false;
            if (rudder > DEAD_ZONE) {
                ModelEngineHelper.playAnimation(model, "spin_right", 0.2, 0.2, 1, false);
            } else if (rudder < -DEAD_ZONE) {
                ModelEngineHelper.playAnimation(model, "spin_left", 0.2, 0.2, 1, false);
            }
            return;
        }

        if (correcting) {
            return;
        }
        if (lastRudder > DEAD_ZONE) {
            ModelEngineHelper.playAnimation(model, "correct_right", 0.2, 0.2, 1, false);
            correcting = true;
        } else if (lastRudder < -DEAD_ZONE) {
            ModelEngineHelper.playAnimation(model, "correct_left", 0.2, 0.2, 1, false);
            correcting = true;
        }
    }

    /**
     * One ambient noise, fired in short bursts rather than as evenly spaced single plays.
     * <p>
     * A burst is two or three plays a fraction of a second apart, then a long silence: a hull that knocks once every
     * two seconds sounds like a machine, whereas one that knocks three times and then goes quiet sounds like a wave
     * passing under it. Pitch is re-rolled per play, so no two plays in a burst are the same hit.
     */
    private static final class AmbientVoice {

        /** Ticks between the plays inside one burst. */
        private static final int MIN_GAP = 2;
        private static final int MAX_GAP = 10;

        private final Sound sound;
        private final float minPitch;
        private final float maxPitch;
        private final float volume;
        private final int maxBurst;
        private final int minRest;
        private final int maxRest;

        /** Ticks until the next play, and how many of the current burst are still owed. */
        private int nextIn;
        private int remaining;

        private AmbientVoice(@NotNull Sound sound, float minPitch, float maxPitch, float volume,
                             int maxBurst, int minRest, int maxRest) {
            this.sound = sound;
            this.minPitch = minPitch;
            this.maxPitch = maxPitch;
            this.volume = volume;
            this.maxBurst = maxBurst;
            this.minRest = minRest;
            this.maxRest = maxRest;
            this.nextIn = UtilMath.randomInt(minRest, maxRest);
        }

        private void tick(@NotNull List<Player> crew) {
            if (--nextIn > 0) {
                return;
            }

            if (remaining <= 0) {
                // Exclusive upper bound, so the longest burst the caller asked for has to be reachable.
                remaining = UtilMath.randomInt(1, maxBurst + 1);
            }

            final SoundEffect effect = new SoundEffect(sound, (float) UtilMath.randDouble(minPitch, maxPitch), volume);
            crew.forEach(effect::play);

            nextIn = --remaining > 0 ? UtilMath.randomInt(MIN_GAP, MAX_GAP) : UtilMath.randomInt(minRest, maxRest);
        }
    }
}
