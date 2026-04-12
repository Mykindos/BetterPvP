package me.mykindos.betterpvp.core.npc.model;

import lombok.Getter;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.behavior.NPCBehavior;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import me.mykindos.betterpvp.core.utilities.model.Ticked;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents any wrapped NPC.
 * <p>
 * Behaviours can be attached via {@link #addBehavior(NPCBehavior)} and are ticked each
 * server tick by {@link me.mykindos.betterpvp.core.npc.controller.NPCTicker}.
 */
@Getter
public abstract class NPC implements Ticked, Actor {

    private static int counter = 0;

    protected final Entity entity;
    protected final int id;
    protected final NPCFactory factory;
    protected final List<Entity> attached = new ArrayList<>();
    private final List<NPCBehavior> behaviors = new ArrayList<>();

    protected NPC(Entity entity, NPCFactory factory) {
        this.entity = entity;
        this.id = counter++;
        this.factory = factory;
    }

    /**
     * Called when a player right-clicks this NPC. Override to implement interaction logic.
     */
    @Override
    public void act(Player runner) {}

    /**
     * Attaches a behaviour to this NPC. {@link NPCBehavior#start()} is called immediately.
     */
    public void addBehavior(NPCBehavior behavior) {
        behaviors.add(behavior);
        behavior.start();
    }

    /**
     * Detaches a behaviour from this NPC. {@link NPCBehavior#stop()} is called immediately.
     */
    public void removeBehavior(NPCBehavior behavior) {
        behavior.stop();
        behaviors.remove(behavior);
    }

    @Override
    public void tick() {
        for (NPCBehavior behavior : behaviors) {
            behavior.tick();
        }
    }

    protected void attachToLifecycle(Entity entity) {
        attached.add(entity);
    }

    public void remove() {
        behaviors.forEach(NPCBehavior::stop);
        factory.getRegistry().unregister(this);
        if (entity != null) {
            entity.remove();
        }

        for (Entity attachedEntity : attached) {
            attachedEntity.remove();
        }
    }

}
