package me.mykindos.betterpvp.core.scene.npc;

import lombok.Getter;
import me.mykindos.betterpvp.core.scene.SceneEntity;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents any wrapped NPC entity in the scene system.
 * <p>
 * Behaviours can be attached via {@link #addBehavior} and are ticked each
 * server tick by {@link me.mykindos.betterpvp.core.scene.controller.SceneTicker}.
 * Multiple behaviours can be stacked on a single NPC - they are all ticked independently.
 * <p>
 * Entity binding is two-phase: construct the NPC first, then call
 * {@link #init(Entity)} once the backing entity is available.
 */
@Getter
public abstract class NPC extends SceneEntity implements Actor {

    protected final NPCFactory factory;

    protected NPC(NPCFactory factory) {
        super();
        this.factory = factory;
    }

    /**
     * @deprecated Use {@link #NPC(NPCFactory)} and call {@link #init(Entity)} separately.
     *             Retained for backward compatibility while existing subclasses are migrated.
     */
    @Deprecated
    protected NPC(@NotNull Entity entity, NPCFactory factory) {
        this(factory);
        init(entity);
    }

    /**
     * Called when a player right-clicks this NPC. Override to implement interaction logic.
     */
    @Override
    public void act(Player runner) {}

}
