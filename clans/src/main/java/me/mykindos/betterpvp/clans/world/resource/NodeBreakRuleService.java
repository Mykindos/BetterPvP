package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.blockbreak.global.GlobalBlockBreakRules;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.RuleLayer;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.zone.PlayerEnterZoneEvent;
import me.mykindos.betterpvp.core.world.zone.PlayerExitZoneEvent;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a resource node's block-break rules to whoever is standing in it, and withdraws them on the way out.
 * <p>
 * Two sources feed in. The node's own {@code fixedSpeed} contributes a {@link RuleLayer#OVERRIDE} rule that replaces
 * the held tool's speed outright, so every pickaxe mines the node at the same rate — the knob that lets a starter mine
 * be paced deliberately instead of by whatever gear the player walked in with. The node's archetype contributes
 * whatever else it needs through {@link ResourceArchetype#breakRules}.
 * <p>
 * Rules are registered <em>per player</em>, which is what makes a per-player rule possible at all: the rule lives in
 * one player's list, so it already knows whose it is and its matcher only has to look at the block.
 * <p>
 * The exact rule instances handed to {@link GlobalBlockBreakRules} are retained here for withdrawal. A rule's condition
 * is a lambda, and lambdas have no value equality, so removal has to pass back the same object rather than an equal
 * one.
 */
@Singleton
@BPvPListener
public class NodeBreakRuleService implements Listener {

    private final Clans clans;
    private final ResourceNodeManager nodes;
    private final ZoneManager zoneManager;
    private final GlobalBlockBreakRules breakRules;

    /** player → zone key → the rule instances registered for that player in that node. */
    private final Map<UUID, Map<Key, List<BlockBreakRule>>> applied = new ConcurrentHashMap<>();

    @Inject
    public NodeBreakRuleService(@NotNull Clans clans, @NotNull ResourceNodeManager nodes,
                                @NotNull ZoneManager zoneManager, @NotNull GlobalBlockBreakRules breakRules) {
        this.clans = clans;
        this.nodes = nodes;
        this.zoneManager = zoneManager;
        this.breakRules = breakRules;
    }

    // Both edges trigger the same reconcile, because the event only ever names the one zone that won on priority -
    // which need not be the node's. See reconcile.
    @EventHandler
    public void onEnterZone(PlayerEnterZoneEvent event) {
        scheduleReconcile(event.getPlayer());
    }

    @EventHandler
    public void onExitZone(PlayerExitZoneEvent event) {
        scheduleReconcile(event.getPlayer());
    }

    /**
     * Reconciles on the next tick rather than inside the event, because a zone change is resolved from the location a
     * player is moving <i>to</i> while the player themselves is still standing at the location they came from.
     */
    private void scheduleReconcile(@NotNull Player player) {
        UtilServer.runTask(clans, () -> {
            if (player.isOnline()) {
                reconcile(player);
            }
        });
    }

    /**
     * Brings a player's registered rules in line with the nodes they are actually standing in.
     * <p>
     * Deliberately not driven by the zone the event names. Zones overlap - a mine inside a spawn safe area is two of
     * them - and only the highest-priority one is reported as entered or left, so reacting to that zone alone would
     * mean a node quietly stopped applying its rules the moment somebody drew a higher-priority region over it. Asking
     * which node zones actually contain the player answers the question the rules care about, whatever else is layered
     * on top.
     */
    private void reconcile(@NotNull Player player) {
        final Set<Key> inside = new HashSet<>();
        for (Zone zone : zoneManager.getZonesAt(player.getLocation())) {
            if (nodes.byZone(zone.getKey()) != null) {
                inside.add(zone.getKey());
            }
        }

        final UUID playerId = player.getUniqueId();
        final Map<Key, List<BlockBreakRule>> held = applied.get(playerId);
        if (held != null) {
            for (Key key : new ArrayList<>(held.keySet())) {
                if (!inside.contains(key)) {
                    withdraw(playerId, key);
                }
            }
        }
        for (Key key : inside) {
            final Map<Key, List<BlockBreakRule>> current = applied.get(playerId);
            if (current == null || !current.containsKey(key)) {
                final ResourceNodeProp node = nodes.byZone(key);
                if (node != null) {
                    apply(player, key, node);
                }
            }
        }
    }

    /**
     * Drops this service's bookkeeping. The rules themselves are already gone — {@code GlobalBlockBreakRulesImpl}
     * clears a player's whole list on quit — so this only prevents the map from leaking entries for players who left
     * without an exit event.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        applied.remove(event.getPlayer().getUniqueId());
    }

    private void apply(@NotNull Player player, @NotNull Key zoneKey, @NotNull ResourceNodeProp node) {
        final UUID playerId = player.getUniqueId();
        // A re-entry without an intervening exit (a teleport within the node, a reload that re-fires entry) would
        // otherwise stack a second copy of every rule on top of the first.
        withdraw(playerId, zoneKey);

        final List<BlockBreakRule> rules = new ArrayList<>();
        if (node.getDefinition().hasFixedSpeed()) {
            rules.add(BlockBreakRule.of(
                    new ZoneBlockMatcher(node.getZone()),
                    BlockBreakProperties.breakable(node.getDefinition().getFixedSpeed()),
                    BlockBreakRule.ALWAYS,
                    RuleLayer.OVERRIDE));
        }
        rules.addAll(node.getArchetype().breakRules(node, player));
        if (rules.isEmpty()) {
            return;
        }

        for (BlockBreakRule rule : rules) {
            breakRules.addRule(playerId, rule);
        }
        applied.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>()).put(zoneKey, rules);
    }

    private void withdraw(@NotNull UUID playerId, @NotNull Key zoneKey) {
        final Map<Key, List<BlockBreakRule>> byZone = applied.get(playerId);
        if (byZone == null) {
            return;
        }
        final List<BlockBreakRule> rules = byZone.remove(zoneKey);
        if (rules != null) {
            for (BlockBreakRule rule : rules) {
                breakRules.removeRule(playerId, rule);
            }
        }
        if (byZone.isEmpty()) {
            applied.remove(playerId);
        }
    }
}
