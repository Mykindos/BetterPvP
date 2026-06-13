package me.mykindos.betterpvp.core.scene.mob;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.scene.HasModeledEntity;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.mob.ai.AIController;
import me.mykindos.betterpvp.core.scene.mob.ai.Navigator;
import me.mykindos.betterpvp.core.scene.mob.animation.AnimationController;
import me.mykindos.betterpvp.core.scene.mob.animation.AnimationProvider;
import me.mykindos.betterpvp.core.scene.mob.animation.AnimationProviders;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import me.mykindos.betterpvp.core.scene.mob.faction.Faction;
import me.mykindos.betterpvp.core.scene.mob.sound.MobSound;
import me.mykindos.betterpvp.core.scene.mob.sound.MobSoundBehavior;
import me.mykindos.betterpvp.core.scene.mob.sound.SoundProvider;
import me.mykindos.betterpvp.core.scene.mob.sound.SoundProviders;
import me.mykindos.betterpvp.core.scene.mob.target.ThreatTable;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for code-driven custom mobs. It is an {@link NPC} (a non-playable character) whose
 * behaviour is composed from swappable AI components arbitrated by an {@link AIController}.
 * <p>
 * <b>Extend this class</b> and configure it in your constructor - set the disposition, faction,
 * model, animations, and activation radius - then override {@link #registerComponents()} to attach
 * the mob's AI components (or override {@link NPC#act(org.bukkit.entity.Player)} for interaction):
 * <pre>{@code
 * public class SentinelMob extends SceneMob {
 *     public SentinelMob(SceneObjectFactory factory) {
 *         super(factory, EntityType.ZOMBIE, Disposition.HOSTILE);
 *         setActivationRadius(40);
 *         setAnimation(MobAnimation.WALK, "walk");                       // one fixed clip
 *         setAnimation(MobAnimation.HURT, AnimationProviders.random("hurt1", "hurt2", "hurt3"));
 *         setAnimation(MobAnimation.IDLE, AnimationProviders.whenTargeting( // varies by state
 *                 AnimationProviders.fixed("idle_combat"),
 *                 AnimationProviders.fixed("idle")));
 *         setSound(MobSound.HURT, SoundProviders.withPitchVariation(           // jittered hurt grunts
 *                 SoundProviders.random(new SoundEffect(Sound.ENTITY_RAVAGER_HURT, 1f)), 0.15f));
 *         setSound(MobSound.DEATH, new SoundEffect(Sound.ENTITY_RAVAGER_DEATH, 1f));
 *     }
 *     @Override protected void registerComponents() {
 *         getAi().add(new TargetingComponent(this, TargetSelectors.nearestEnemy(40)));
 *         getAi().add(new MeleeAttackComponent(this));
 *     }
 * }
 * }</pre>
 * A spawner constructs the mob, spawns a backing entity of {@link #getEntityType()}, then calls
 * {@code factory.spawn(mob, entity)} (two-phase init + registration).
 * <p>
 * The vanilla brain is disabled via {@link Mob#setAware(boolean)} so the components are the sole
 * driver, while {@link Navigator} (manual pathfinding) still works. Mobs run on a bare vanilla
 * entity; setting a {@link #setModelId(String) modelId} binds a ModelEngine model and hides the
 * vanilla entity. Ticking is gated on player proximity ({@link #getActivationRadius()}).
 * <p>
 * <b>Eager, not chunk-managed.</b> Unlike static props and NPCs, a mob is spawned via the eager
 * {@code factory.spawn(mob, entity)} path and is deliberately left out of chunk-driven materialization. A roaming
 * combat entity wanders away from its spawn anchor, so anchor-keyed respawn would diverge from where it actually is;
 * and its lifetime belongs to the encounter/spawner that created it, which decides when it (re)spawns. On chunk unload
 * it simply despawns (non-persistent) and the owning system re-spawns it as appropriate.
 */
@Getter
public class SceneMob extends NPC implements HasModeledEntity {

    /** ModelEngine clip name that signals the mob is dying; while it plays the mob is treated as dead. */
    private static final String DEATH_CLIP = "death";

    /** Vanilla entity used to host this mob in the world (spawned by the factory before init). */
    private final EntityType entityType;

    @Setter private Disposition disposition;
    @Setter @Nullable private Faction faction;
    @Setter @Nullable private String modelId;
    @Setter private double activationRadius = 48.0;

    /**
     * Colour the model flashes on hurt, applied only when {@link #damageTintEnabled}. Defaults to red
     * (ModelEngine's own fallback); override with {@code setDamageTint(Color)} to tint per mob.
     */
    @Setter @Nullable private Color damageTint = Color.RED;

    /** Owning player/clan member, for FRIENDLY owned mobs. {@code null} for unowned mobs. */
    @Setter @Nullable private UUID owner;

    /** The entity this mob is currently focusing, written by the targeting component. */
    @Setter @Nullable private LivingEntity currentTarget;

    /** Logical-state -> clip resolver. Populate via {@link #setAnimation}. */
    private final Map<MobAnimation, AnimationProvider> animationProviders = new EnumMap<>(MobAnimation.class);

    /** Logical-cue -> sound resolver. Populate via {@link #setSound}. */
    private final Map<MobSound, SoundProvider> soundProviders = new EnumMap<>(MobSound.class);

    private final AIController ai = new AIController();
    private final ThreatTable threat = new ThreatTable();

    private Navigator navigator;
    private AnimationController animations;
    private MobSoundBehavior sounds;
    private Location homeAnchor;

    // Activation-gate state. The proximity check is sampled (not every tick) to keep it cheap.
    private boolean active = true;
    private boolean wasActive = true;
    private int activationCheckCounter = 0;

    public SceneMob(SceneObjectFactory factory, EntityType entityType, Disposition disposition) {
        super(factory);
        this.entityType = entityType;
        this.disposition = disposition;
        // Created here (not in onInit) so subclasses can tune it fluently in their constructor via
        // getSounds(); it reads the shared soundProviders map at play time, so setSound order is free.
        this.sounds = new MobSoundBehavior(this, soundProviders);
        addBehavior(this.sounds);
    }

    /**
     * Maps a logical animation state to a single fixed ModelEngine clip - the common case. Shorthand
     * for {@code setAnimation(animation, AnimationProviders.fixed(animationId))}. Call in the constructor.
     */
    protected void setAnimation(MobAnimation animation, String animationId) {
        setAnimation(animation, AnimationProviders.fixed(animationId));
    }

    /**
     * Maps a logical animation state to an {@link AnimationProvider} that chooses the concrete clip
     * at play time based on the mob's state - use for multi-clip states (hurt1..hurt4) or
     * state-dependent variations (idle vs idle_combat). See {@link AnimationProviders} for ready-made
     * strategies. Call in the constructor.
     */
    protected void setAnimation(MobAnimation animation, AnimationProvider provider) {
        animationProviders.put(animation, provider);
    }

    /**
     * Maps a logical sound cue to a single fixed {@link SoundEffect} - the common case. Shorthand for
     * {@code setSound(sound, SoundProviders.fixed(soundEffect))}. Call in the constructor.
     */
    protected void setSound(MobSound sound, SoundEffect soundEffect) {
        setSound(sound, SoundProviders.fixed(soundEffect));
    }

    /**
     * Maps a logical sound cue to a {@link SoundProvider} that chooses the concrete effect at play time
     * based on the mob's state - use for multi-sound cues (random hurt grunts) or state-dependent
     * variations (combat snarl vs idle grunt). See {@link SoundProviders} for ready-made strategies.
     * Call in the constructor.
     */
    protected void setSound(MobSound sound, SoundProvider provider) {
        soundProviders.put(sound, provider);
    }

    /** Override to attach this mob's AI components. Called once after controllers are ready. */
    protected void registerComponents() {
    }

    @Override
    protected void onInit() {
        boolean bound = false;
        if (modelId != null) {
            final ModeledEntity modeledEntity = ModelEngineHelper.bind(getEntity());
            final ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
            modeledEntity.addModel(activeModel, true);
            // ModelEngine flashes the damage tint off the host entity's vanilla hurt ticks, so real
            // DamageEvents drive it automatically once enabled - no per-hit hook needed.
            if (damageTint != null) {
                activeModel.setCanHurt(true);
                activeModel.setDamageTint(damageTint);
            }
            bound = true;
        }

        if (getEntity() instanceof Mob bukkitMob) {
            bukkitMob.setAware(true);
            final MobGoals goals = Bukkit.getMobGoals();
            goals.removeAllGoals(bukkitMob);
            if (bound) {
                // The ModelEngine model is the visual - hide the vanilla host entity.
                bukkitMob.setInvisible(true);
                bukkitMob.setSilent(true);
            }
        }
        this.navigator = new Navigator(this);
        this.animations = new AnimationController(this, animationProviders);
        this.homeAnchor = getEntity().getLocation();
        registerComponents();
    }

    @Override
    public void tick() {
        active = computeActive();

        if (active) {
            ai.tick();
            // Re-resolve the held looping animation so state-dependent variants swap live. Runs after
            // the AI tick so it reflects any state the components just changed (e.g. acquiring a target).
            animations.tick();
        } else if (wasActive) {
            // Just went out of range - halt cleanly, drop references, and stop ticking AI.
            ai.stopAll();
            navigator.stop();
            currentTarget = null;
            threat.clear();
        }
        wasActive = active;

        super.tick(); // existing SceneBehaviors (nameplates, etc.)
    }

    /**
     * Decides whether the mob should drive its AI this tick. A mob is active only while alive, with a
     * player within {@link #activationRadius} (sampled ~once per second to stay cheap), and not already
     * playing its death clip.
     */
    private boolean computeActive() {
        if (entity == null || entity.isDead()) {
            return false; // dead entities are never active, even if players are nearby
        }

        if (activationCheckCounter-- <= 0) {
            activationCheckCounter = 20; // re-check proximity ~once per second
            active = !getEntity().getWorld().getNearbyPlayers(getEntity().getLocation(), activationRadius).isEmpty();
        }
        if (!active) {
            return false;
        }

        // If the entity is playing the dead animation it is about to die, so treat it as inactive.
        final ModeledEntity modeledEntity = getModeledEntity();
        if (modeledEntity != null) {
            for (ActiveModel model : modeledEntity.getModels().values()) {
                if (model.getAnimationHandler().isPlayingAnimation(DEATH_CLIP)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Disposition getDisposition() {
        return disposition;
    }

    /**
     * Paths toward a (possibly moving) entity and holds the WALK clip. Safe to call every tick - the
     * navigator re-targets a moving goal and {@link AnimationController#play} no-ops when WALK is
     * already held, so callers don't need to track a "moving" flag themselves.
     */
    public void startMoving(LivingEntity target, double speed) {
        navigator.moveTo(target, speed);
        animations.play(MobAnimation.WALK);
    }

    /** Paths toward a fixed point and holds the WALK clip. See {@link #startMoving(LivingEntity, double)}. */
    public void startMoving(Location target, double speed) {
        navigator.moveTo(target, speed);
        animations.play(MobAnimation.WALK);
    }

    /** Halts pathfinding and drops back to the IDLE clip. Call when a movement behaviour ends or yields. */
    public void stopMoving() {
        navigator.stop();
        animations.play(MobAnimation.IDLE);
    }

    /** @return {@code true} if the target is non-null, alive, still valid, and in this mob's world. */
    public boolean isValidTarget(@Nullable LivingEntity target) {
        return target != null && !target.isDead() && target.isValid()
                && target.getWorld().equals(getEntity().getWorld());
    }

    /** @return the owning player if it is online and in this mob's world, otherwise {@code null}. */
    @Nullable
    public Player getActiveOwner() {
        if (owner == null) {
            return null;
        }
        final Player player = Bukkit.getPlayer(owner);
        if (player == null || !player.isOnline() || !player.getWorld().equals(getEntity().getWorld())) {
            return null;
        }
        return player;
    }

    /** @return the backing entity as a {@link Mob}, or {@code null} if it isn't one. */
    @Nullable
    public Mob getBukkitMob() {
        return isInitialized() && getEntity() instanceof Mob bukkitMob ? bukkitMob : null;
    }

    @Override
    @Nullable
    public ModeledEntity getModeledEntity() {
        if (!isInitialized()) {
            return null;
        }
        return ModelEngineAPI.getModeledEntity(getEntity());
    }

    @Override
    public void remove() {
        ai.stopAll();
        currentTarget = null;
        threat.clear();
        final ModeledEntity modeledEntity = getModeledEntity();
        if (modeledEntity != null) {
            modeledEntity.markRemoved();
        }
        super.remove();
    }

}
