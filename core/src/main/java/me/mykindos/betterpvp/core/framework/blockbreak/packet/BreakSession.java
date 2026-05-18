package me.mykindos.betterpvp.core.framework.blockbreak.packet;

import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.block.SmartBlockBreakOverride;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import org.bukkit.block.data.BlockData;

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

    // The tick loop re-derived smart-block override, breakability/speed, and
    // smart-block status every tick. All three depend only on the block's
    // BlockData (plus the held item, which already ends the session on change),
    // so we memoize them and recompute only when the BlockData changes
    // (Vein Echo respawn, falling block, turning to AIR, etc.).
    private BlockData resolvedFor;
    @Getter private SmartBlockBreakOverride cachedSmartOverride;
    @Getter private boolean cachedIsSmartBlock;
    /** Null until resolved for the current {@link #resolvedFor}; cleared when BlockData changes. */
    @Getter @Setter private BlockBreakProperties cachedProps;

    public BreakSession(UUID playerId, UUID worldUid, Vector3i blockPos, int animationEntityId) {
        this.playerId = playerId;
        this.worldUid = worldUid;
        this.blockPos = blockPos;
        this.animationEntityId = animationEntityId;
        this.startedAtNanos = System.nanoTime();
    }

    /** True if the override/smart-block cache is valid for {@code data}. */
    public boolean isResolvedFor(BlockData data) {
        return resolvedFor != null && resolvedFor.equals(data);
    }

    /**
     * Store the override + smart-block status for {@code data} and invalidate the
     * (lazily computed, ordering-sensitive) {@link #cachedProps}.
     */
    public void cacheResolve(BlockData data, SmartBlockBreakOverride smartOverride, boolean isSmartBlock) {
        this.resolvedFor = data;
        this.cachedSmartOverride = smartOverride;
        this.cachedIsSmartBlock = isSmartBlock;
        this.cachedProps = null;
    }
}
