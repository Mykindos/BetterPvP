package me.mykindos.betterpvp.core.npc.behavior;

/**
 * A discrete, composable unit of behaviour attached to an NPC.
 * <p>
 * Behaviours are ticked each server tick via {@link me.mykindos.betterpvp.core.npc.controller.NPCTicker}.
 * Multiple behaviours can be stacked on a single NPC — they are all ticked independently.
 */
public interface NPCBehavior {

    /**
     * Called every server tick while this behaviour is attached to an NPC.
     */
    void tick();

    /**
     * Called once when this behaviour is added to an NPC via
     * {@link me.mykindos.betterpvp.core.npc.model.NPC#addBehavior(NPCBehavior)}.
     */
    default void start() {}

    /**
     * Called once when this behaviour is removed from an NPC, or when the NPC is removed.
     */
    default void stop() {}

}
