package me.mykindos.betterpvp.core.world.construction;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * A structure type named after its id with a capital first letter, with three free ten-minute stages whose builds are
 * named {@code <id>_<stage>}, and the upgrades it is given.
 */
public final class TestStructureType implements StructureType {

    private final String id;
    private final List<StructureUpgrade> upgrades;

    public TestStructureType(@NotNull String id, @NotNull StructureUpgrade... upgrades) {
        this.id = id;
        this.upgrades = List.of(upgrades);
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text(Character.toUpperCase(id.charAt(0)) + id.substring(1));
    }

    @Override
    public int getTier() {
        return 0;
    }

    @Override
    public @NotNull Set<String> getRequiredStructures() {
        return Set.of();
    }

    @Override
    public @Nullable String getRequiredZoneTag() {
        return null;
    }

    @Override
    public @NotNull List<StructureStage> getStages() {
        return List.of(
                new StructureStage(id + "_0", ResourceCost.NONE, Duration.ofMinutes(10)),
                new StructureStage(id + "_1", ResourceCost.NONE, Duration.ofMinutes(10)),
                new StructureStage(id + "_2", ResourceCost.NONE, Duration.ofMinutes(10)));
    }

    @Override
    public @NotNull StructureFlags getFlags() {
        return StructureFlags.builder().build();
    }

    @Override
    public @NotNull List<StructureUpgrade> getUpgrades() {
        return upgrades;
    }
}
