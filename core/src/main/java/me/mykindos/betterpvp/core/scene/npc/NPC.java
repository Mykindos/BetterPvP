package me.mykindos.betterpvp.core.scene.npc;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.scene.SceneEntity;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

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

    protected final SceneObjectFactory factory;

    /**
     * What happens when a player right-clicks this NPC, when the interaction is supplied rather than
     * inherited. This is what lets an NPC be assembled entirely from data - model, nameplate, patrol
     * and interaction all attached to a plain {@link NPC} - instead of requiring a subclass per
     * character just to override {@link #act(Player)}.
     */
    @Setter @Nullable private Consumer<Player> interactionHandler;

    protected NPC(SceneObjectFactory factory) {
        super();
        this.factory = factory;
    }

    /**
     * @deprecated Use {@link #NPC(SceneObjectFactory)} and call {@link #init(Entity)} separately.
     *             Retained for backward compatibility while existing subclasses are migrated.
     */
    @Deprecated
    protected NPC(@NotNull Entity entity, SceneObjectFactory factory) {
        this(factory);
        init(entity);
    }

    /**
     * Called when a player right-clicks this NPC. Runs the {@link #setInteractionHandler assigned
     * handler} if there is one; override instead when the interaction belongs to a subclass.
     */
    @Override
    public void act(Player runner) {
        if (interactionHandler != null) {
            interactionHandler.accept(runner);
        }
    }

}
