package me.mykindos.betterpvp.clans.world.resource;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the {@code chains} block shared by every ore-shaped archetype, and answers which of the parsed chains are
 * <i>resources</i>.
 * <p>
 * Kept apart from any one archetype because the chain model is the node's data, not a mechanic: a globally mined field
 * and a per-player one disagree about where a block's current stage is stored but agree completely about what the
 * chains mean.
 */
public final class DegradeChains {

    private DegradeChains() {
    }

    /**
     * Parses {@code chains}: a map of named chains, where the key is a readable label carrying no behaviour. Chains
     * shorter than two stages are dropped (nothing to degrade to).
     * <p>
     * A chain is written either as a bare list of stages, or as a block with the stages under {@code stages} plus
     * chain-level options beside them:
     * <pre>
     *   copper: [copper_ore, stone]          # a bare list - every option at its default
     *   gravel:                              # a block, for a chain that sets one
     *     timer: false
     *     stages: [gravel, stone]
     * </pre>
     * Both forms mean the same thing where they overlap, so no existing node file has to be rewritten to use the
     * second.
     * <p>
     * A stage is either a plain material string or an object with {@code material}, an optional {@code lootTable}, and
     * an optional {@code unbreakable} flag.
     */
    public static @NotNull List<DegradeChain> parse(@Nullable ConfigurationSection root) {
        final List<DegradeChain> chains = new ArrayList<>();
        if (root == null) {
            return chains;
        }
        final ConfigurationSection chainsSection = root.getConfigurationSection("chains");
        if (chainsSection == null) {
            return chains;
        }
        for (String name : chainsSection.getKeys(false)) {
            final DegradeChain chain = parseChain(chainsSection.get(name));
            if (chain != null) {
                chains.add(chain);
            }
        }
        return chains;
    }

    /**
     * Maps every chain's first (intact) material to that chain — each chain head is a <i>resource</i> that is
     * snapshotted and respawns. This includes a head that another chain also degrades into (e.g. {@code stone}, the head
     * of the erosion chain that {@code copper}/{@code diamond} step down to), so its blocks are snapshotted and respawn
     * rather than eroding permanently.
     */
    public static @NotNull Map<Material, DegradeChain> resourceMaterials(@NotNull List<DegradeChain> chains) {
        final Map<Material, DegradeChain> resources = new HashMap<>();
        for (DegradeChain chain : chains) {
            final Material material = Material.matchMaterial(chain.first());
            if (material != null) {
                resources.putIfAbsent(material, chain);
            }
        }
        return resources;
    }

    private static @Nullable DegradeChain parseChain(@Nullable Object entry) {
        if (entry instanceof ConfigurationSection section) {
            return parseChain(section.getList("stages"), section.getBoolean("timer", true));
        }
        if (entry instanceof Map<?, ?> map) {
            return parseChain(map.get("stages"), !Boolean.FALSE.equals(map.get("timer")));
        }
        return parseChain(entry, true);
    }

    private static @Nullable DegradeChain parseChain(@Nullable Object entry, boolean showTimer) {
        if (!(entry instanceof List<?> list) || list.size() < 2) {
            return null;
        }
        final List<DegradeChain.Stage> stages = new ArrayList<>(list.size());
        for (Object element : list) {
            stages.add(parseStage(element));
        }
        return DegradeChain.of(stages, showTimer);
    }

    private static @NotNull DegradeChain.Stage parseStage(@Nullable Object element) {
        if (element instanceof Map<?, ?> map) {
            final Object lootTable = map.get("lootTable");
            return new DegradeChain.Stage(String.valueOf(map.get("material")),
                    lootTable == null ? null : lootTable.toString(),
                    Boolean.TRUE.equals(map.get("unbreakable")));
        }
        return new DegradeChain.Stage(String.valueOf(element), null, false);
    }
}
