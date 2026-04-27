package me.mykindos.betterpvp.core.framework.blockbreak.packet;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Drives server-authoritative block-break progress via PacketEvents animation packets.
 * <p>
 * Implementation notes:
 * <ul>
 *   <li>One {@link BreakSession} per (player, block).</li>
 *   <li>Animation packet stages are only re-sent when the integer stage changes
 *       (10 stages over the entire break duration, regardless of break time).</li>
 *   <li>Multiple players may simultaneously dig the same block; each gets a
 *       unique synthetic entity id so their overlays don't clobber each other.</li>
 * </ul>
 */
public interface BlockBreakProgressService {

    /** Begin tracking a dig for {@code player} at the block at {@code worldUid}+pos. */
    void startSession(@NotNull Player player, @NotNull UUID worldUid, int x, int y, int z);

    /** Cancel any active session this player has. Sends stage -1 to clear overlay. */
    void cancelSessionFor(@NotNull UUID playerId);

    /** Cancel all sessions on a specific block (e.g. when one digger completes it). */
    void cancelSessionsAt(@NotNull UUID worldUid, int x, int y, int z);
}
