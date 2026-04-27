package me.mykindos.betterpvp.core.framework.blockbreak.global;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@BPvPListener
@Singleton
public class GlobalBlockBreakRulesImpl implements GlobalBlockBreakRules, Listener {

    private final ConcurrentHashMap<UUID, List<BlockBreakRule>> rulesByPlayer = new ConcurrentHashMap<>();

    @Override
    public void addRule(@NotNull UUID playerId, @NotNull BlockBreakRule rule) {
        rulesByPlayer.compute(playerId, (id, existing) -> {
            final List<BlockBreakRule> list = existing == null ? new ArrayList<>() : existing;
            for (BlockBreakRule e : list) {
                if (rule.matcher().overlaps(e.matcher())) {
                    final Set<Material> a = rule.matcher().knownMaterials();
                    final Set<Material> b = e.matcher().knownMaterials();
                    throw new IllegalArgumentException(
                            "Global rule conflict for " + id + ": new=" + a + " existing=" + b);
                }
            }
            list.add(rule);
            return list;
        });
    }

    @Override
    public void removeRule(@NotNull UUID playerId, @NotNull BlockBreakRule rule) {
        rulesByPlayer.computeIfPresent(playerId, (id, list) -> {
            list.remove(rule);
            return list.isEmpty() ? null : list;
        });
    }

    @Override
    public void clear(@NotNull UUID playerId) {
        rulesByPlayer.remove(playerId);
    }

    @Override
    public @NotNull List<BlockBreakRule> getRules(@NotNull UUID playerId) {
        final List<BlockBreakRule> list = rulesByPlayer.get(playerId);
        return list == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
    }

    @Override
    public Optional<BlockBreakRule> resolve(@NotNull UUID playerId, @NotNull Block block) {
        final List<BlockBreakRule> list = rulesByPlayer.get(playerId);
        if (list == null) return Optional.empty();
        // snapshot to avoid CME if mutation happens mid-iteration
        for (BlockBreakRule r : new ArrayList<>(list)) {
            if (r.matcher().matches(block)) return Optional.of(r);
        }
        return Optional.empty();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }
}
