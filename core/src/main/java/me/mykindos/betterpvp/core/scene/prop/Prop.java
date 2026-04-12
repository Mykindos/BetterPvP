package me.mykindos.betterpvp.core.scene.prop;

import lombok.Getter;
import me.mykindos.betterpvp.core.scene.SceneEntity;

/**
 * A non-interactive, static world object managed by the scene system.
 * <p>
 * Props follow the same two-phase init contract as {@link me.mykindos.betterpvp.core.npc.model.NPC}:
 * construct first (stores configuration), then call {@link #init(org.bukkit.entity.Entity)} once a
 * backing entity is available. Subclasses override {@link #onInit()} to perform entity-dependent setup
 * (model attachment, nameplate spawning, etc.).
 * <p>
 * Child entities (e.g. decorative displays attached to the prop's visual) can be registered
 * via {@link #attachToLifecycle(org.bukkit.entity.Entity)} so they are automatically removed
 * with the prop.
 * <p>
 * Like NPCs, props support {@link me.mykindos.betterpvp.core.scene.behavior.SceneBehavior}s
 * via {@link #addBehavior} - for example, {@link me.mykindos.betterpvp.core.npc.behavior.BoneTagBehavior}
 * or {@link me.mykindos.betterpvp.core.npc.behavior.AnimationSequenceBehavior}.
 * {@link me.mykindos.betterpvp.core.npc.behavior.WaypointPatrolBehavior} is intentionally
 * NPC-only and should not be applied to props.
 *
 * @see ModeledProp
 */
@Getter
public abstract class Prop extends SceneEntity {

    protected final PropFactory factory;

    protected Prop(PropFactory factory) {
        super();
        this.factory = factory;
    }
}
