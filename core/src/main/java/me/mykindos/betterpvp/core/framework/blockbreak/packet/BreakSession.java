package me.mykindos.betterpvp.core.framework.blockbreak.packet;

import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Per-(player, block) mining state. Intentionally lightweight and mutable —
 * accessed from both the packet listener thread and the main-thread tick loop.
 * Mutation is confined to the tick loop; the listener thread only inserts
 * new sessions and removes them.
 */
@Getter
public final class BreakSession {

    private final UUID playerId;
    private final UUID worldUid;
    private final Vector3i blockPos;
    private final int animationEntityId;
    private final long startedAtNanos;

    /** progress in [0, 1]; ≥1 triggers completion next tick. */
    @Setter private double progress;
    /** integer destruction stage last sent (-1 = no overlay rendered yet). */
    @Setter private int lastStageSent = -1;

    public BreakSession(UUID playerId, UUID worldUid, Vector3i blockPos, int animationEntityId) {
        this.playerId = playerId;
        this.worldUid = worldUid;
        this.blockPos = blockPos;
        this.animationEntityId = animationEntityId;
        this.startedAtNanos = System.nanoTime();
    }
}
