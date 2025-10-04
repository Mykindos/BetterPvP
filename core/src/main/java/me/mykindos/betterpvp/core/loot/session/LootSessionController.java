package me.mykindos.betterpvp.core.loot.session;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.LootTable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Stack-based controller: each player has an ordered stack of scopes.
 * Top of the stack has highest precedence.
 */
@Singleton
@BPvPListener
public class LootSessionController implements Listener {

    public static final Map<LootTable, LootSession> GLOBAL_SCOPE = new HashMap<>();
    private final Map<UUID, Deque<LootSession>> playerScopes = new HashMap<>();

    /** Push a new scope on top (takes precedence). */
    void pushScope(Player player, LootSession node) {
        playerScopes
                .computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>())
                .push(node);
    }

    /** Remove a scope from the stack. */
    void removeScope(Player player, LootSession node) {
        playerScopes.get(player.getUniqueId()).remove(node);
    }

    public @NotNull LootSession getGlobalScope(LootTable table) {
        final LootSession session = GLOBAL_SCOPE.get(table);
        Preconditions.checkNotNull(session, "No global session for table %s", table.getId());
        return session;
    }

    /**
     * Resolve a loot session for the given table.
     * Checks stack top → bottom. First match wins.
     */
    public @NotNull LootSession resolveOrGlobal(Player player, LootTable table) {
        Deque<LootSession> stack = playerScopes.get(player.getUniqueId());
        if (stack != null) {
            // Check for started scopes
            for (LootSession node : stack) {
                if (node.getLootTables().contains(table)) {
                    return node;
                }
            }
        }

        return GLOBAL_SCOPE.get(table);
    }

    /**
     * Resolve a loot session for the given table, or create a new one if none exists.
     * @param player  The player to resolve the session for.
     * @param table The table to resolve the session for.
     * @param supplier A supplier for a new session if one does not exist. You are in charge of pushing this scope
     *                 when it is generated.
     * @return The loot session for the given table.
     */
    public @NotNull LootSession resolve(Player player, LootTable table, Supplier<LootSession> supplier) {
        Deque<LootSession> stack = playerScopes.get(player.getUniqueId());
        if (stack != null) {
            // Check for started scopes
            for (LootSession node : stack) {
                if (node.getLootTables().contains(table)) {
                    return node;
                }
            }
        }

        return supplier.get();
    }

    /**
     * Resolve a loot session for the given table by a string identifier. This method
     * will match any loot table with the given identifier in its name.
     * @param player The player to resolve the session for.
     * @param contained The identifier to match against.
     * @return The loot session for the given table.
     */
    public @NotNull LootSession resolve(Player player, String contained) {
        Deque<LootSession> stack = playerScopes.get(player.getUniqueId());
        if (stack != null) {
            // Check for started scopes
            for (LootSession node : stack) {
                if (node.getLootTables().stream().anyMatch(table -> table.getId().contains(contained))) {
                    return node;
                }
            }
        }

        return GLOBAL_SCOPE.values().stream()
                .filter(session -> session.getLootTables().stream().anyMatch(table -> table.getId().contains(contained)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No global session for table containing " + contained));
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerScopes.put(player.getUniqueId(), new ArrayDeque<>());
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerScopes.remove(player.getUniqueId());
    }
}
