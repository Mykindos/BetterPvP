package me.mykindos.betterpvp.clans.world.camp.structure;

import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureFlags;
import me.mykindos.betterpvp.core.world.construction.StructureStage;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.StructureUpgrade;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    private final CampUpgrades upgrades;

    CampStructure(@NotNull String id, int tier, @NotNull Set<String> required, @Nullable String zoneTag,
                  @NotNull StructureFlags flags, @NotNull CampConfig config, @NotNull CampUpgrades upgrades) {
        this.id = id;
        this.tier = tier;
        this.required = Set.copyOf(required);
        this.zoneTag = zoneTag;
        this.flags = flags;
        this.config = config;
        this.upgrades = upgrades;
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

    /** What it is called at {@code stage}, 0 being the first. Players see these, never a stage number. */
    public @NotNull Component stageName(int stage) {
        return Translations.component("clans.camp.structure." + id + ".stage." + (stage + 1));
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
    public @NotNull List<StructureStage> getStages() {
        return numbers().getStages();
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

    /** The upgrades registered for it, with their numbers from {@code camps.yml}. Stage order, then registration order. */
    @Override
    public @NotNull List<StructureUpgrade> getUpgrades() {
        final Map<String, CampConfig.UpgradeNumbers> numbers = numbers().getUpgrades();
        return upgrades.declared(id).stream().map(declared -> {
            final CampConfig.UpgradeNumbers found = numbers.get(declared.getId());
            return found == null
                    ? new StructureUpgrade(declared.getId(), declared.getStage(), ResourceCost.NONE, Duration.ZERO, 0, null)
                    : new StructureUpgrade(declared.getId(), declared.getStage(), found.getCost(), found.getTime(),
                    found.getWorkforce(), found.getPiece());
        }).toList();
    }

    /** What the menus show an upgrade as. */
    public @NotNull Material upgradeIcon(@NotNull String upgrade) {
        final CampConfig.UpgradeNumbers found = numbers().getUpgrades().get(upgrade);
        return found == null ? Material.ANVIL : found.getIcon();
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
                List.of(new StructureStage("camps/" + id, ResourceCost.NONE, Duration.ZERO)),
                ResourceCost.NONE, Duration.ZERO, ResourceCost.NONE, Duration.ZERO, 0, Material.BRICKS, Map.of()));
    }
}
