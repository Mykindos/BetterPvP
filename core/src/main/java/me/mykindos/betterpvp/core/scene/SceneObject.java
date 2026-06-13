package me.mykindos.betterpvp.core.scene;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for all server-managed world objects (NPCs, props, text displays, etc.).
 * <p>
 * A scene object is a <b>persistent logical actor</b>; its backing {@link Entity} is an <b>ephemeral
 * materialization</b>. Because scene entities are non-persistent (see {@link CoreNamespaceKeys#SCENE_OBJECT}), the
 * server discards them whenever their chunk's entities unload - so the object must be able to spawn its body again when
 * the chunk reloads. This is the difference between a <em>despawn</em> (chunk unload) and a <em>death</em>
 * ({@link #remove()}): the former leaves the object registered and dormant, ready to re-materialize; the latter is
 * permanent.
 *
 * <h3>Two ways an object gets its entity</h3>
 * <ul>
 *   <li><b>Chunk-managed</b> (preferred for anything outside force-loaded chunks): call
 *       {@link #configureMaterialization(Location, Function)} before registering. The object stores <em>where</em> it
 *       lives and <em>how</em> to build its body, and {@link SceneMaterializationController} spawns/despawns it as the
 *       anchor chunk's entities load and unload. Survives chunk cycling indefinitely.</li>
 *   <li><b>Eager / externally-bound</b> (legacy, and correct for force-loaded or packet-only objects): call
 *       {@link #init(Entity)} with an already-spawned entity. The object activates once and is never chunk-cycled
 *       ({@link #isChunkManaged()} is {@code false}). Used by force-loaded hub/shop NPCs, combat {@code SceneMob}s, and
 *       packet {@code HumanNPC}s.</li>
 * </ul>
 *
 * <h3>Lifecycle hooks</h3>
 * {@link #onInit()} runs every time the entity (re)appears - bind a ModelEngine model, style a display, add behaviours.
 * {@link #onDematerialize()} runs every time the entity goes away - the framework already stops behaviours and removes
 * attached children for {@link SceneEntity}; override only to release entity-bound resources (e.g. a ModelEngine model).
 */
@Getter
public abstract class SceneObject {

    /** Whether the object's backing entity is currently present in the world. */
    public enum LifecycleState { DORMANT, ACTIVE }

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final int id;
    @Nullable protected Entity entity;

    // Set by SceneObjectRegistry on register(); cleared on unregister().
    // Package-private so only the registry can write it.
    @Nullable SceneObjectRegistry registry;

    /** Where this object lives. Non-null once {@link #configureMaterialization} ran; drives chunk indexing. */
    @Nullable private Location anchor;

    /** How to (re)spawn the backing entity at the anchor. Non-null only for self-spawning chunk-managed objects. */
    @Nullable private Function<Location, Entity> entityFactory;

    private LifecycleState state = LifecycleState.DORMANT;

    /** {@code true} when the live entity was created by {@link #entityFactory} (so dematerialize may remove it). */
    private boolean ownsEntity;

    /**
     * Decorations applied on every materialization, after {@link #onInit()}. Lets a loader attach presentation (e.g. a
     * nameplate) to an object generically without that decoration being lost when the entity is re-spawned. Kept across
     * the object's logical lifetime; the behaviours/entities they create are transient and torn down on dematerialize.
     */
    private final List<Consumer<SceneObject>> decorators = new ArrayList<>();

    protected SceneObject() {
        this.id = COUNTER.getAndIncrement();
    }

    /**
     * Registers a decoration re-applied on every materialization. If the object is already active, it is applied once
     * immediately so callers don't have to special-case timing.
     *
     * @param decorator receives this object with its entity present; typically casts to {@link SceneEntity} to attach a
     *                  behaviour or nameplate
     */
    public final void addDecorator(@NotNull Consumer<SceneObject> decorator) {
        decorators.add(decorator);
        if (state == LifecycleState.ACTIVE) {
            decorator.accept(this);
        }
    }

    /**
     * Configures this object for chunk-managed materialization. Call <b>before</b> registering, instead of
     * {@link #init(Entity)}. The object stays {@link LifecycleState#DORMANT} until
     * {@link SceneMaterializationController} materializes it.
     *
     * @param anchor        where the object lives (used to index it by chunk)
     * @param entityFactory builds the backing entity at the anchor; called on every materialization. Pass {@code null}
     *                      if the entity is supplied another way (e.g. a packet NPC binds its handle via
     *                      {@link #bindEntity(Entity)} and only its decorations are chunk-managed).
     */
    public final void configureMaterialization(@NotNull Location anchor, @Nullable Function<Location, Entity> entityFactory) {
        this.anchor = anchor;
        this.entityFactory = entityFactory;
    }

    /**
     * Binds the backing entity without activating the object. For chunk-managed objects whose body exists independently
     * of the chunk lifecycle (e.g. a packet {@code HumanNPC}'s world-less handle): the body is bound once here, while
     * {@link #materialize()}/{@link #dematerialize()} drive only the object's decorations.
     */
    protected final void bindEntity(@NotNull Entity entity) {
        this.entity = entity;
    }

    /**
     * Eager bind: attaches an already-spawned, externally-owned entity and activates the object immediately. The object
     * is <b>not</b> chunk-managed - use this only for force-loaded or packet-only objects. Calling it after the object
     * is already active throws.
     *
     * @param entity the entity that represents this object in the world
     */
    public final void init(@NotNull Entity entity) {
        if (state == LifecycleState.ACTIVE) {
            throw new IllegalStateException("SceneObject #" + id + " is already active");
        }
        this.entity = entity;
        this.ownsEntity = false;
        activate();
    }

    /**
     * Spawns the backing entity (if self-spawning) and activates the object. Idempotent: a no-op if already active.
     * Called by {@link SceneMaterializationController} when the anchor chunk's entities load.
     */
    public final void materialize() {
        if (state == LifecycleState.ACTIVE) {
            return;
        }
        if (entity == null && entityFactory != null && anchor != null) {
            this.entity = entityFactory.apply(anchor);
            this.ownsEntity = true;
        }
        if (entity == null) {
            return; // nothing to show - misconfigured; stays dormant
        }
        activate();
    }

    /** Stamps the entity, runs the per-materialization hook, and flips to ACTIVE. */
    private void activate() {
        entity.getPersistentDataContainer().set(CoreNamespaceKeys.SCENE_OBJECT, PersistentDataType.BOOLEAN, true);
        entity.setPersistent(false);
        onInit();
        for (Consumer<SceneObject> decorator : decorators) {
            decorator.accept(this);
        }
        state = LifecycleState.ACTIVE;
    }

    /**
     * Tears down the backing entity and its behaviours but keeps the object registered and dormant, ready to
     * re-materialize. Called by {@link SceneMaterializationController} when the anchor chunk's entities unload.
     */
    public final void dematerialize() {
        if (state != LifecycleState.ACTIVE) {
            return;
        }
        onDematerialize();
        if (ownsEntity && entity != null) {
            entity.remove();
            entity = null;
            ownsEntity = false;
        }
        state = LifecycleState.DORMANT;
    }

    /**
     * Hook invoked immediately after the entity is bound, on <b>every</b> materialization. Override to perform setup
     * that requires the entity to be present (model binding, display styling, behaviour attachment).
     */
    protected void onInit() {}

    /**
     * Hook invoked immediately before the entity is released, on every dematerialization. The framework already stops
     * behaviours and removes attached child entities for {@link SceneEntity}; override to release entity-bound
     * resources (e.g. {@code ModeledEntity#markRemoved()}).
     */
    protected void onDematerialize() {}

    /**
     * @return whether this object's entity lifetime is driven by chunk load/unload. {@code true} once
     * {@link #configureMaterialization} has run.
     */
    public boolean isChunkManaged() {
        return anchor != null;
    }

    /**
     * Returns the backing entity.
     *
     * @throws IllegalStateException if no entity is currently bound (the object is dormant or uninitialized)
     */
    @NotNull
    public Entity getEntity() {
        if (entity == null) {
            throw new IllegalStateException("SceneObject #" + id + " has no entity (dormant or uninitialized)");
        }
        return entity;
    }

    public boolean isInitialized() {
        return entity != null;
    }

    public boolean isMaterialized() {
        return state == LifecycleState.ACTIVE;
    }

    public boolean isRegistered() {
        return registry != null;
    }

    /**
     * Permanently removes this object: tears down its entity and behaviours and unregisters it. Unlike
     * {@link #dematerialize()} the object will not come back. Subclasses should call {@code super.remove()} after their
     * own cleanup.
     */
    public void remove() {
        if (state == LifecycleState.ACTIVE) {
            onDematerialize();
        }
        if (entity != null) {
            entity.remove();
            entity = null;
        }
        ownsEntity = false;
        state = LifecycleState.DORMANT;
        if (registry != null) {
            registry.unregister(this);
        }
    }

}
