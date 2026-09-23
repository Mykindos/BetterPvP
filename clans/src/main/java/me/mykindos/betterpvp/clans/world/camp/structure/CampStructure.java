package me.mykindos.betterpvp.clans.world.camp.structure;

import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureFlags;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.StructureVersion;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * A camp structure: what it is and what it allows are code, how much it costs and how long it takes are read from
 * {@code camps.yml} each time they are asked for, so a config reload changes them straight away.
 */
public final class CampStructure implements StructureType {

    private final String id;
    private final int tier;
    private final Set<String> required;
    private final @Nullable String zoneTag;
    private final StructureFlags flags;
    private final CampConfig config;

    CampStructure(@NotNull String id, int tier, @NotNull Set<String> required, @Nullable String zoneTag,
                  @NotNull StructureFlags flags, @NotNull CampConfig config) {
        this.id = id;
        this.tier = tier;
        this.required = Set.copyOf(required);
        this.zoneTag = zoneTag;
        this.flags = flags;
        this.config = config;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("clans.camp.structure." + id + ".name");
    }

    public @NotNull Component getDescription() {
        return Translations.component("clans.camp.structure." + id + ".description");
    }

    public @NotNull Material getIcon() {
        return numbers().getIcon();
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    public @NotNull Set<String> getRequiredStructures() {
        return required;
    }

    @Override
    public @Nullable String getRequiredZoneTag() {
        return zoneTag;
    }

    @Override
    public @NotNull List<StructureVersion> getVersions() {
        return numbers().getVersions();
    }

    @Override
    public @NotNull StructureFlags getFlags() {
        return flags.toBuilder().demolishRefund(numbers().getDemolishRefund()).build();
    }

    @Override
    public @NotNull ResourceCost getMoveCost() {
        return numbers().getMoveCost();
    }

    @Override
    public @NotNull Duration getMoveTime() {
        return numbers().getMoveTime();
    }

    @Override
    public @NotNull ResourceCost getRepairCost() {
        return numbers().getRepairCost();
    }

    @Override
    public @NotNull Duration getRepairTime() {
        return numbers().getRepairTime();
    }

    private @NotNull CampConfig.StructureNumbers numbers() {
        return config.structure(id).orElseGet(() -> new CampConfig.StructureNumbers(
                List.of(new StructureVersion("camps/" + id, ResourceCost.NONE, Duration.ZERO)),
                ResourceCost.NONE, Duration.ZERO, ResourceCost.NONE, Duration.ZERO, 0, Material.BRICKS));
    }
}
