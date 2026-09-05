package me.mykindos.betterpvp.clans.world.resource;

import lombok.Value;

/**
 * One depleted block of a node that is on its way back, and how far along it is.
 * <p>
 * The unit {@link ResourceNodeTimerService} renders. Deliberately carries no notion of <em>whose</em> countdown it is:
 * the archetype answering the question already knows whether its state is shared or per player, so a caller can ask
 * "what is regrowing for this viewer" and get the right answer either way without knowing which kind of node it is
 * looking at.
 * <p>
 * {@code totalMs} rides along with {@code remainingMs} so a renderer can show progress without having to look up the
 * node's configuration, or reason about the speed multipliers already folded into both.
 */
@Value
public class RespawnPoint {

    int x;
    int y;
    int z;
    long remainingMs;
    long totalMs;
}
