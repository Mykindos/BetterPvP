package me.mykindos.betterpvp.core.scene.mob;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.scene.HasModeledEntity;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.mob.ai.AIController;
import me.mykindos.betterpvp.core.scene.mob.ai.Navigator;
import me.mykindos.betterpvp.core.scene.mob.ai.component.AttendComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.OrderToSpotComponent;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

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
 * The vanilla goals are removed so the components are the sole driver, while {@link Navigator} (manual
 * pathfinding) still works. While the AI runs the body's own AI is on so it can path. Mobs run on a bare vanilla
 * entity; setting a {@link #setModelId(String) modelId} binds a ModelEngine model and hides the
 * vanilla entity. Ticking is gated on player proximity ({@link #getActivationRadius()}).
 * <p>
 * A mob is usually spawned eagerly with {@code factory.spawn(mob, entity)}, and the encounter or spawner that created
 * it decides when it (re)spawns. A mob that stays near its anchor can instead be chunk-managed with
 * {@link #configureMaterialization}. Its body is then spawned again on each chunk load, set up again from
 * {@link #onInit()}, and its AI starts from scratch. On unload its components stop and it stops ticking.
 */
@Getter
public class SceneMob extends NPC implements HasModeledEntity {

    /** ModelEngine clip name that signals the mob is dying; while it plays the mob is treated as dead. */
    private static final String DEATH_CLIP = "death";

    @Getter(AccessLevel.NONE) private final LongSupplier clock;

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
    /** The follow range the body gets while the AI runs, so the pathfinder can plan trips this long. */
    @Setter private double pathRange = 48.0;
    /** Pathfinding speed multiplier used on the way to a spot it is {@link #orderTo ordered to}. */
    @Setter private double orderSpeed = 1.0;

    // Activation-gate state. The proximity check is sampled (not every tick) to keep it cheap.
    private boolean active = true;
    private boolean aiRunning;
    private boolean enabledBodyAi;
    private int activationCheckCounter = 0;

    @Getter(AccessLevel.NONE) private AttendComponent attending;
    @Getter(AccessLevel.NONE) private OrderToSpotComponent orders;

    public SceneMob(SceneObjectFactory factory, EntityType entityType, Disposition disposition) {
        this(factory, entityType, disposition, System::currentTimeMillis);
    }

    protected SceneMob(SceneObjectFactory factory, EntityType entityType, Disposition disposition, LongSupplier clock) {
        super(factory);
        this.clock = clock;
        this.entityType = entityType;
        this.disposition = disposition;
        // Created here (not in onInit) so subclasses can tune it fluently in their constructor via
        // getSounds(); it reads the shared soundProviders map at play time, so setSound order is free.
        // It is attached in onInit, because every despawn clears the behaviours.
        this.sounds = new MobSoundBehavior(this, soundProviders);
    }

    /**
     * Maps a logical animation state to a single fixed ModelEngine clip - the common case. Shorthand
     * for {@code setAnimation(animation, AnimationProviders.fixed(animationId))}. Call in the constructor, or later to
     * change the look of a mob that is already spawned.
     */
    public void setAnimation(MobAnimation animation, String animationId) {
        setAnimation(animation, AnimationProviders.fixed(animationId));
    }

    /**
     * Maps a logical animation state to an {@link AnimationProvider} that chooses the concrete clip
     * at play time based on the mob's state - use for multi-clip states (hurt1..hurt4) or
     * state-dependent variations (idle vs idle_combat). See {@link AnimationProviders} for ready-made
     * strategies. Call in the constructor, or later to change the look of a mob that is already spawned. A held looping
     * state picks up a changed clip on the next tick.
     */
    public void setAnimation(MobAnimation animation, AnimationProvider provider) {
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

    /** Override to attach this mob's AI components. Called after controllers are ready, on every spawn. */
    protected void registerComponents() {
    }

    @Override
    protected void onInit() {
        ai.clear();
        currentTarget = null;
        threat.clear();
        active = true;
        aiRunning = false;
        enabledBodyAi = false;
        activationCheckCounter = 0;

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
        addBehavior(sounds);
        registerComponents();
        orders = new OrderToSpotComponent(this, clock).speed(orderSpeed);
        attending = new AttendComponent(this, clock);
        ai.addFirst(orders);
        ai.addFirst(attending);
    }

    @Override
    public void tick() {
        active = computeActive();

        if (active) {
            if (!aiRunning) {
                startRuntime();
            }
            ai.tick();
            navigator.tick();
            // Re-resolve the held looping animation so state-dependent variants swap live. Runs after
            // the AI tick so it reflects any state the components just changed (e.g. acquiring a target).
            animations.tick();
        } else if (aiRunning) {
            stopRuntime();
        }

        super.tick(); // existing SceneBehaviors (nameplates, etc.)
    }

    /** Hands the body to the AI: its own AI on, no vanilla goals, and a follow range that covers a trip. */
    private void startRuntime() {
        aiRunning = true;
        final Mob bukkitMob = getBukkitMob();
        if (bukkitMob == null) {
            return;
        }
        if (!bukkitMob.hasAI()) {
            bukkitMob.setAI(true);
            enabledBodyAi = true;
        }
        Bukkit.getMobGoals().removeAllGoals(bukkitMob);
        final AttributeInstance followRange = bukkitMob.getAttribute(Attribute.FOLLOW_RANGE);
        if (followRange != null && followRange.getBaseValue() < pathRange) {
            followRange.setBaseValue(pathRange);
        }
    }

    /** Halts cleanly, drops references and gives the body back its AI setting. */
    private void stopRuntime() {
        aiRunning = false;
        ai.stopAll();
        navigator.stop();
        currentTarget = null;
        threat.clear();
        final Mob bukkitMob = getBukkitMob();
        if (enabledBodyAi && bukkitMob != null) {
            bukkitMob.setAI(false);
        }
        enabledBodyAi = false;
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

    /**
     * Starts a trip to a fixed point and holds the WALK clip. The trip searches again when it has no path or the body
     * stops moving. See {@link Navigator#travelTo}.
     */
    public void travelTo(Location target, double speed, Runnable onGiveUp) {
        navigator.travelTo(target, speed, onGiveUp);
        animations.play(MobAnimation.WALK);
    }

    /** Halts pathfinding and drops back to the IDLE clip. Call when a movement behaviour ends or yields. */
    public void stopMoving() {
        navigator.stop();
        animations.play(MobAnimation.IDLE);
    }

    /** Stops pathing, faces {@code player} and holds IDLE for a moment, then lets the AI decide again. */
    public void attend(Player player) {
        attending.attend(player);
    }

    /** Sends the mob to a random point near {@code spot}, rests there, then lets the AI decide again. */
    public void orderTo(Location spot) {
        orders.orderTo(spot);
    }

    /** Stops every running component so each decides again on the next tick. */
    public void replan() {
        ai.replan();
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
    protected void onDematerialize() {
        stopRuntime();
        final ModeledEntity modeledEntity = getModeledEntity();
        if (modeledEntity != null) {
            modeledEntity.markRemoved();
        }
        super.onDematerialize();
    }

}
