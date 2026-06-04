package me.mykindos.betterpvp.clans.world.resource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * An ordered sequence of block-material {@link Stage stages} a mined node steps through, e.g. {@code copper_ore → stone
 * → cobblestone → deepslate}. Stage materials are normalised (lower-cased, {@code minecraft:} namespace stripped) so
 * they compare cleanly against a {@code BlockData} material key. Pure and Bukkit-free for testing.
 * <p>
 * Each stage carries its own optional {@link Stage#lootTable() loot table} (rolled when a block at that stage is mined;
 * a null table falls back to the block's vanilla drops) and its own {@link Stage#unbreakable()} flag — an unbreakable
 * stage can never be mined and is the seam {@code ResourceNodeManager} uses to cancel the {@code BlockDamageEvent}. A
 * node may carry several chains at once; {@code OreArchetype} resolves which one governs a given block.
 */
public final class DegradeChain {

    private final List<Stage> stages;

    private DegradeChain(@NotNull List<Stage> stages) {
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("A degrade chain needs at least one stage");
        }
        this.stages = List.copyOf(stages);
    }

    /**
     * @return a chain over plain materials with no per-stage loot or unbreakable flag (tests and the legacy single
     * {@code chain} config form)
     */
    public static @NotNull DegradeChain ofMaterials(@NotNull List<String> materials) {
        return new DegradeChain(materials.stream().map(material -> new Stage(material, null, false)).toList());
    }

    public static @NotNull DegradeChain of(@NotNull List<Stage> stages) {
        return new DegradeChain(stages);
    }

    /**
     * @return the next stage's material after {@code current}, or empty if {@code current} is unknown or terminal
     */
    public @NotNull Optional<String> next(@NotNull String current) {
        final int index = indexOf(current);
        if (index < 0 || index >= stages.size() - 1) {
            return Optional.empty();
        }
        return Optional.of(stages.get(index + 1).material());
    }

    /**
     * @return true if {@code current} is the final stage or not part of this chain (nothing left to degrade to)
     */
    public boolean isTerminal(@NotNull String current) {
        return next(current).isEmpty();
    }

    /**
     * @return the stage matching {@code current}'s material, or empty if {@code current} is not part of this chain
     */
    public @NotNull Optional<Stage> stageOf(@NotNull String current) {
        final int index = indexOf(current);
        return index < 0 ? Optional.empty() : Optional.of(stages.get(index));
    }

    /**
     * @return the first (intact) stage's material — what the node looks like when fully respawned
     */
    public @NotNull String first() {
        return stages.get(0).material();
    }

    public @NotNull List<Stage> stages() {
        return stages;
    }

    private int indexOf(@NotNull String material) {
        final String normalised = normalise(material);
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).material().equals(normalised)) {
                return i;
            }
        }
        return -1;
    }

    public static @NotNull String normalise(@NotNull String material) {
        String value = material.trim().toLowerCase(Locale.ROOT);
        final int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(colon + 1);
        }
        return value;
    }

    /**
     * One stage of a chain: a block material plus the loot and breakability that apply while a block sits at it. The
     * material is normalised on construction.
     */
    public static final class Stage {

        private final String material;
        private final @Nullable String lootTable;
        private final boolean unbreakable;

        public Stage(@NotNull String material, @Nullable String lootTable, boolean unbreakable) {
            this.material = normalise(material);
            this.lootTable = lootTable == null || lootTable.isBlank() ? null : lootTable;
            this.unbreakable = unbreakable;
        }

        public @NotNull String material() {
            return material;
        }

        /**
         * @return the loot-table id rolled when a block at this stage is mined, or null to drop the block's vanilla loot
         */
        public @Nullable String lootTable() {
            return lootTable;
        }

        /**
         * @return true if a block at this stage can never be mined (its {@code BlockDamageEvent} is cancelled)
         */
        public boolean unbreakable() {
            return unbreakable;
        }
    }
}
