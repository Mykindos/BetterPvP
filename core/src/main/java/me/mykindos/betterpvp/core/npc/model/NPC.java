package me.mykindos.betterpvp.core.npc.model;

import lombok.Getter;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents any wrapped NPC
 */
@Getter
public abstract class NPC {

    private static int counter = 0;

    protected final Entity entity;
    protected final int id;
    protected final NPCFactory factory;
    protected final List<Entity> attached = new ArrayList<>();

    protected NPC(Entity entity, NPCFactory factory) {
        this.entity = entity;
        this.id = counter++;
        this.factory = factory;
    }

    protected void attachToLifecycle(Entity entity) {
        attached.add(entity);
    }

    public void remove() {
        factory.getRegistry().unregister(this);
        if (entity != null) {
            entity.remove();
        }

        for (Entity attachedEntity : attached) {
            attachedEntity.remove();
        }
    }

}
