package me.mykindos.betterpvp.core.scene.mob.animation;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Maps a {@link SceneMob}'s logical {@link MobAnimation} states to concrete ModelEngine clips and
 * hands them to ModelEngine's {@code AnimationHandler}. <b>ModelEngine owns the animation state</b> -
 * which clips are playing, blending between them, and per-clip priority (configured in the model) -
 * so this class stays thin: it resolves <i>which</i> clip a state should play (via the state's
 * {@link AnimationProvider}) and holds the current looping state so it can keep re-resolving it.
 * <p>
 * It does track one thing about "what is playing": the concrete clip id the held looping state last
 * resolved to. ModelEngine layers animations and does <b>not</b> stop one clip when another starts,
 * so when the resolved looping clip changes (walk &rarr; walk_combat, or walk &rarr; idle) the
 * controller must {@linkplain ModelEngineHelper#stopAnimation stop} the outgoing clip itself -
 * otherwise the old locomotion clip keeps looping <i>underneath</i> the new one and the two blend
 * (e.g. the mob appears stuck in its combat walk, or shows attack-pose legs while walking). Swaps
 * crossfade via a short lerp rather than snapping. One-shots layer over the looping base and are
 * left to end on their own.
 * <p>
 * Three ways to drive it, freely mixed:
 * <ul>
 *   <li>{@link #play(MobAnimation)} - request a logical state. A {@linkplain MobAnimation#isLooping()
 *       looping} state (IDLE, WALK) is <i>held</i> and re-resolved every {@link #tick()} so
 *       state-dependent variants (walk &rarr; walk_combat) swap live. The tick refresh re-evaluates
 *       provider <i>conditions</i> but does not re-roll a non-deterministic pick (a held
 *       {@code random}/{@code sequential} clip keeps looping rather than restarting every tick); a
 *       fresh pick is only rolled on the next explicit {@code play}. Re-requests are cheap because
 *       ModelEngine ignores a clip already playing. A one-shot state (ATTACK, HURT) fires once.</li>
 *   <li>{@link #force(MobAnimation)} / {@link #force(String)} - play <b>now</b>, restarting/overriding
 *       even if that clip is already playing. Use for reactions that must replay or animations you
 *       want to assert immediately. It still obeys ModelEngine's priority blending, so a held looping
 *       clip resumes underneath once a higher-priority forced clip finishes.</li>
 *   <li>{@link #play(String)} / {@link #play(String, double)} - play <b>any</b> raw clip cooperatively
 *       (won't restart if already playing). For mob-specific animations (roars, casts, taunts).</li>
 * </ul>
 * Every method is placeholder-friendly: if the mob has no bound model, the call is a no-op, so AI
 * components and interaction code can request animations without checking. You can also bypass this
 * class entirely and drive {@link SceneMob#getModeledEntity()} directly.
 */
public class AnimationController {

    /** Crossfade time (seconds) blended in when a clip starts and out when it is stopped. */
    private static final double BLEND_SECONDS = 0.2;

    private final SceneMob mob;
    private final Map<MobAnimation, AnimationProvider> providers;

    /** The looping state currently held, re-resolved each {@link #tick()}. {@code null} until set. */
    @Nullable
    private MobAnimation loopingState;

    /**
     * The concrete clip the held looping state last resolved to, so a changed resolution stops the
     * outgoing clip before the incoming one starts. {@code null} when nothing is looping.
     */
    @Nullable
    private String loopingClip;

    public AnimationController(SceneMob mob, Map<MobAnimation, AnimationProvider> providers) {
        this.mob = mob;
        this.providers = providers;
    }

    /**
     * Requests a logical state. Looping states are remembered and re-resolved each tick; one-shot
     * states fire once. No-op (beyond any built-in cue) if the state resolves to no clip or the mob
     * has no bound model.
     */
    public void play(MobAnimation animation) {
        if (animation.isLooping()) {
            // Only a switch into this state is a fresh re-entry (re-roll a non-deterministic pick);
            // re-requesting the state already held is a refresh, so a per-tick caller (a mob held in
            // WALK while pathing) keeps its clip looping instead of restarting it every tick.
            final boolean reentry = animation != loopingState;
            loopingState = animation;
            apply(animation, false, reentry);
        } else {
            // One-shot reaction: force so a repeat hit replays it rather than being deduped away.
            apply(animation, true, true);
        }
    }

    /**
     * Re-resolves and re-applies the held looping state, picking up provider <i>condition</i> decisions
     * that depend on live mob state (e.g. walk vs walk_combat once a target is acquired) without
     * re-rolling a non-deterministic variant - the held pick is kept so the clip keeps looping instead
     * of restarting. Driven by {@link SceneMob} each active tick; cheap because unchanged clips are
     * deduped by ModelEngine.
     */
    public void tick() {
        if (loopingState != null) {
            // Routine refresh, not a re-entry: condition-bearing providers re-evaluate so variants swap
            // live, but non-deterministic leaves keep their last pick so the clip is not restarted each
            // tick (which would leave it stuck on its first frame).
            apply(loopingState, false, false);
        }
    }

    /**
     * Forcefully (re)plays a logical state now, overriding/restarting even if its clip is already
     * playing. A looping state is also held afterwards (so {@link #tick()} keeps it).
     */
    public void force(MobAnimation animation) {
        if (animation.isLooping()) {
            loopingState = animation;
        }
        apply(animation, true, true);
    }

    /** Forcefully (re)plays any raw clip now, overriding/restarting if already playing. */
    public void force(String animationId) {
        force(animationId, 1.0);
    }

    /** Forcefully (re)plays any raw clip now at the given speed, overriding/restarting if playing. */
    public void force(String animationId, double speed) {
        playClip(animationId, BLEND_SECONDS, BLEND_SECONDS, speed, true);
    }

    /** Plays any animation by its raw ModelEngine id, cooperatively. No-op if no model is bound. */
    public void play(String animationId) {
        play(animationId, 1.0);
    }

    /** Plays any animation by its raw ModelEngine id at the given speed, cooperatively. */
    public void play(String animationId, double speed) {
        playClip(animationId, BLEND_SECONDS, BLEND_SECONDS, speed, false);
    }

    /** @return whether the mob has a model bound (animations will actually play). */
    public boolean hasModel() {
        return mob.getModeledEntity() != null;
    }

    /** Resolves the state's provider to a clip and plays it, falling back to a built-in cue if unmapped. */
    private void apply(MobAnimation animation, boolean force, boolean reentry) {
        final AnimationProvider provider = providers.get(animation);
        final String animationId = provider != null ? provider.resolve(mob, reentry) : null;

        if (animation.isLooping()) {
            applyLooping(animationId, force);
            return;
        }

        if (animationId != null) {
            // One-shot layered over the looping base; it is ONCE/HOLD in the model so it ends itself.
            playClip(animationId, BLEND_SECONDS, BLEND_SECONDS, 1.0, force);
            return;
        }

        // No clip resolved for this state - fall back to a built-in cue where one makes sense.
        if (animation == MobAnimation.ATTACK) {
            final int id = mob.getEntity().getEntityId();
            final WrapperPlayServerEntityStatus wrapper = new WrapperPlayServerEntityStatus(id, (byte) 4);
            for (Player player : mob.getEntity().getTrackedBy()) {
                PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(wrapper);
            }
        }
    }

    /**
     * Drives the single held looping clip. When the resolved clip changes, the outgoing clip is
     * stopped (lerping out) so it does not keep looping underneath the incoming one - the two
     * crossfade instead of stacking. An unchanged clip is re-requested cooperatively, which
     * ModelEngine dedupes to a no-op (or a forced restart when {@code force}).
     */
    private void applyLooping(@Nullable String animationId, boolean force) {
        if (!Objects.equals(animationId, loopingClip) && loopingClip != null) {
            stopClip(loopingClip);
        }
        loopingClip = animationId;
        if (animationId != null) {
            playClip(animationId, BLEND_SECONDS, BLEND_SECONDS, 1.0, force);
        }
    }

    private void playClip(String animationId, double lerpIn, double lerpOut, double speed, boolean force) {
        final ModeledEntity modeledEntity = mob.getModeledEntity();
        if (modeledEntity == null) {
            return;
        }
        for (ActiveModel model : modeledEntity.getModels().values()) {
            ModelEngineHelper.playAnimation(model, animationId, lerpIn, lerpOut, speed, force);
        }
    }

    private void stopClip(String animationId) {
        final ModeledEntity modeledEntity = mob.getModeledEntity();
        if (modeledEntity == null) {
            return;
        }
        for (ActiveModel model : modeledEntity.getModels().values()) {
            ModelEngineHelper.stopAnimation(model, animationId);
        }
    }

}
