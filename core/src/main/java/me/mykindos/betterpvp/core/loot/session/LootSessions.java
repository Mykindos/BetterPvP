package me.mykindos.betterpvp.core.loot.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.loot.LootProgress;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controls sessions for players and their loot.
 *
 * Supports:
 * <ul>
 *     <li>Normal sessions (per-player)</li>
 *     <li>Pooled sessions (shared progress across players)</li>
 *     <li>Bound sessions (tied to a specific loot table)</li>
 *     <li>Pooled + Bound</li>
 * </ul>
 */
@RequiredArgsConstructor
public class LootSessions {

    private final LoadingCache<@NotNull UUID, LootSession> sessions = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(uuid -> new LootSession(new LootProgress(Bukkit.getPlayer(uuid))));
    private final LootSession pooledSession = new LootSession(new LootProgress(null));
    private final boolean pooled;;

    public static LootSessions pooled() {
        return new LootSessions(true);
    }

    public static LootSessions playerBound() {
        return new LootSessions(false);
    }

    public LootSession getPooledSession() {
        return pooledSession;
    }

    public LootSession getSession(@Nullable Player player) {
        if (pooled || player == null) {
            return this.getPooledSession();
        }
        return this.sessions.get(player.getUniqueId());
    }
}
