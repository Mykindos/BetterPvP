package me.mykindos.betterpvp.core.scene;

import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import me.mykindos.betterpvp.core.utilities.model.Ticked;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all scene entities - NPCs, props, and any world object
 * that may carry {@link SceneBehavior}s and/or have child entities attached to its lifecycle.
 * <p>
 * Extends {@link SceneObject} with three shared concerns:
 * <ul>
 *   <li><b>Behavior composition</b>: attach/detach {@link SceneBehavior}s via
 *       {@link #addBehavior}/{@link #removeBehavior}.</li>
 *   <li><b>Tick dispatch</b>: {@link #tick()} forwards to every attached behavior.</li>
 *   <li><b>Child lifecycle</b>: entities registered via {@link #attachToLifecycle} are
 *       removed automatically when this entity is removed.</li>
 * </ul>
 * Concrete subclasses ({@link me.mykindos.betterpvp.core.npc.model.NPC},
 * {@link me.mykindos.betterpvp.core.scene.prop.Prop}) only need to define what makes
 * them distinct (interaction, model binding, etc.).
 */
public abstract class SceneEntity extends SceneObject implements Ticked {

    protected final List<Entity> attached = new ArrayList<>();
    private final List<SceneBehavior> behaviors = new ArrayList<>();

    protected SceneEntity() {
        super();
    }

    /**
     * Attaches a behaviour to this entity. {@link SceneBehavior#start()} is called immediately.
     */
    public void addBehavior(SceneBehavior behavior) {
        behaviors.add(behavior);
        behavior.start();
    }

    /**
     * Detaches a behaviour from this entity. {@link SceneBehavior#stop()} is called immediately.
     */
    public void removeBehavior(SceneBehavior behavior) {
        behavior.stop();
        behaviors.remove(behavior);
    }

    /**
     * Ticks all attached behaviors. Called every server tick by
     * {@link me.mykindos.betterpvp.core.scene.controller.SceneTicker}.
     */
    @Override
    public void tick() {
        for (SceneBehavior behavior : behaviors) {
            behavior.tick();
        }
    }

    /**
     * Registers a child entity to be removed automatically when this entity is removed.
     * Typically used for decorative display entities spawned in {@link #onInit()}.
     */
    protected void attachToLifecycle(Entity entity) {
        attached.add(entity);
    }

    @Override
    public void remove() {
        behaviors.forEach(SceneBehavior::stop);
        behaviors.clear();
        for (Entity attachedEntity : attached) {
            attachedEntity.remove();
        }
        attached.clear();
        super.remove();
    }
}
