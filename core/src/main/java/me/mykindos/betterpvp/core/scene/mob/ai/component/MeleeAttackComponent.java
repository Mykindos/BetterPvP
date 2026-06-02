package me.mykindos.betterpvp.core.scene.mob.ai.component;

import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.cause.VanillaDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.scene.behavior.ModelEngineScriptDispatcher;
import me.mykindos.betterpvp.core.scene.behavior.ModelEngineScriptHandler;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import me.mykindos.betterpvp.core.scene.mob.sound.MobSound;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Chases the mob's current target and strikes it on a cooldown when within melee range.
 * Movement and the WALK clip are only (re)started on the transition into movement so the
 * animation is not restarted every tick while pathing.
 * <p>
 * After committing a strike the mob stays rooted for {@link #freezeMillis} (defaulting to the
 * attack {@link #cooldownMillis}), planting its feet for the swing rather than instantly chasing a
 * target that steps away. Set it to {@code 0L} to disable.
 * <p>
 * <b>Strike timing</b> decides when the damage actually lands relative to the swing's start, so a
 * slow or telegraphed attack can connect mid-animation instead of on frame one:
 * <ul>
 *   <li><b>Instant</b> (default) - lands the moment the swing starts.</li>
 *   <li><b>Timed</b> - {@code windupMillis(ms)} delays the hit by a fixed wind-up.</li>
 *   <li><b>Keyframe</b> - {@code strikeKeyframe("hit")} lands the hit when the attack animation fires
 *       the {@code betterpvp:hit} script keyframe.</li>
 * </ul>
 * A timed/keyframe strike re-checks that the target is still alive and in range when it lands, so a
 * dodged swing whiffs.
 * <p>
 * Tuning fields default to sensible values and can be overridden fluently at attach time,
 * e.g. {@code getAi().add(new MeleeAttackComponent(this).damage(8).cooldownMillis(800))}.
 */
@Accessors(fluent = true, chain = true)
public class MeleeAttackComponent implements AIComponent {

    private final SceneMob mob;

    /** Distance (in blocks) within which the mob will strike rather than keep chasing. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double attackRange = 2.5;
    /** Damage dealt per successful strike. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double damage = 4.0;
    /** Minimum delay between strikes, in milliseconds. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private long cooldownMillis = VanillaDamageCause.DEFAULT_DELAY;
    /** Pathfinding speed multiplier used while chasing the target. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double chaseSpeed = 1.0;
    /**
     * How long, in milliseconds, the mob stays rooted in place after a strike. {@code null} (the
     * default) ties it to {@link #cooldownMillis}; {@code 0L} disables freezing.
     */
    @Setter
    @Nullable
    @Range(from = 0, to = Long.MAX_VALUE)
    private Long freezeMillis = null;
    /**
     * Wind-up before the strike lands, in milliseconds, measured from the swing's start. {@code 0}
     * (the default) lands the hit instantly. Ignored when {@link #strikeKeyframe} is set.
     */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private long windupMillis = 0L;
    /**
     * If set, the strike lands when the attack animation fires the ModelEngine script keyframe
     * {@code betterpvp:<strikeKeyframe>} rather than on the {@link #windupMillis} timer. {@code null}
     * (the default) uses the timer. Lets a slow swing connect on the exact frame the model "hits".
     */
    @Setter
    @Nullable
    private String strikeKeyframe = null;

    private long lastAttack = 0L;
    /** Target committed by the current swing, awaiting its strike (wind-up or keyframe). */
    @Nullable
    private LivingEntity pendingTarget;
    /** When the current swing began, for the {@link #windupMillis} timer. */
    private long swingStartMillis = 0L;
    /** Keyframe handlers registered with the dispatcher while active (one per model). */
    private final List<ModelEngineScriptHandler> scriptHandlers = new ArrayList<>();
    /** Lazily resolved keyframe dispatcher, reused across (un)registration. */
    @Nullable
    private ModelEngineScriptDispatcher scriptDispatcher;

    public MeleeAttackComponent(SceneMob mob) {
        this.mob = mob;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.MOVE, AIControl.LOOK);
    }

    @Override
    public boolean canStart() {
        return mob.getCurrentTarget() != null;
    }

    @Override
    public boolean shouldContinue() {
        return mob.isValidTarget(mob.getCurrentTarget());
    }

    @Override
    public void tick() {
        final long now = System.currentTimeMillis();

        // Land a queued timed strike the moment its wind-up elapses - done first so it still resolves
        // while the mob is frozen mid-swing (the freeze check below returns early during the wind-up).
        if (pendingTarget != null && strikeKeyframe == null && now - swingStartMillis >= windupMillis) {
            strike();
        }

        final LivingEntity target = mob.getCurrentTarget();
        if (target == null) {
            return;
        }

        // Just struck: stay rooted for the freeze window instead of immediately re-chasing.
        if (now - lastAttack < effectiveFreezeMillis()) {
            mob.stopMoving();
            return;
        }

        if (!inRange(target)) {
            mob.startMoving(target, chaseSpeed);
            return;
        }

        if (now - lastAttack >= cooldownMillis) {
            beginSwing(now, target);
        }
    }

    /** @return whether {@code target} is within {@link #attackRange} of the mob's hitbox. */
    private boolean inRange(LivingEntity target) {
        return mob.getEntity().getBoundingBox().expand(attackRange).overlaps(target.getBoundingBox());
    }

    /** Commits a swing: roots the mob, plays the attack cue, and lands the hit now if it is instant. */
    private void beginSwing(long now, LivingEntity target) {
        lastAttack = now;
        swingStartMillis = now;
        pendingTarget = target;
        mob.getAnimations().play(MobAnimation.ATTACK);
        mob.getSounds().play(MobSound.ATTACK);
        // Instant attacks land now; timed/keyframe swings land later (tick timer or onScript).
        if (strikeKeyframe == null && windupMillis == 0L) {
            strike();
        }
    }

    @Override
    public void start() {
        if (strikeKeyframe != null) {
            registerScriptHandlers();
        }
    }

    @Override
    public void stop() {
        pendingTarget = null;
        // Drop back to the resting clip so a mob that loses its target mid-chase doesn't keep
        // looping its walk while standing still (matches ReturnHome/FollowOwner on stop).
        mob.stopMoving();
        unregisterScriptHandlers();
    }

    /** Resolves the freeze duration, falling back to {@link #cooldownMillis} when left unset. */
    private long effectiveFreezeMillis() {
        return freezeMillis != null ? freezeMillis : cooldownMillis;
    }

    /**
     * Applies the committed swing's damage, re-validating that the target is still alive and within
     * reach so a wind-up/keyframe strike whiffs if the target died or stepped out during the swing.
     */
    private void strike() {
        final LivingEntity target = pendingTarget;
        pendingTarget = null;
        if (!mob.isValidTarget(target) || !inRange(target)) {
            return;
        }
        final LivingEntity entity = (LivingEntity) mob.getEntity();
        UtilDamage.doDamage(new DamageEvent(
                target,
                entity,
                entity,
                new VanillaDamageCause(EntityDamageEvent.DamageCause.ENTITY_ATTACK),
                damage
        ));
    }

    /** Registers a keyframe handler per active model so {@code betterpvp:<strikeKeyframe>} lands the hit. */
    private void registerScriptHandlers() {
        final ModeledEntity modeled = mob.getModeledEntity();
        if (modeled == null) {
            return;
        }
        final ModelEngineScriptDispatcher dispatcher = dispatcher();
        for (ActiveModel model : modeled.getModels().values()) {
            final ModelEngineScriptHandler handler = new ModelEngineScriptHandler() {
                @Override
                public ActiveModel getModel() {
                    return model;
                }

                @Override
                public void onScript(IAnimationProperty property, String script) {
                    if (script.equals(strikeKeyframe) && pendingTarget != null) {
                        strike();
                    }
                }
            };
            dispatcher.register(handler);
            scriptHandlers.add(handler);
        }
    }

    private void unregisterScriptHandlers() {
        if (scriptHandlers.isEmpty()) {
            return;
        }
        scriptHandlers.forEach(dispatcher()::unregister);
        scriptHandlers.clear();
    }

    /** Resolves the keyframe dispatcher once and caches it for the component's lifetime. */
    private ModelEngineScriptDispatcher dispatcher() {
        if (scriptDispatcher == null) {
            scriptDispatcher = JavaPlugin.getPlugin(Core.class)
                    .getInjector().getInstance(ModelEngineScriptDispatcher.class);
        }
        return scriptDispatcher;
    }

}
