package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Value;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceKind;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureStage;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The numbers camps run on, read from {@code camps.yml}. */
@Singleton
@CustomLog
public class CampConfig implements Reloadable {

    private final Clans clans;

    @Getter
    private int chestCapacity;
    @Getter
    private int claimLayers;
    private final Map<String, Deposit> deposits = new HashMap<>();
    private final Map<ResourceKind, Material> overflowItems = new EnumMap<>(ResourceKind.class);
    private final Map<ClanMember.MemberRank, Set<ConstructionAction>> permissions =
            new EnumMap<>(ClanMember.MemberRank.class);
    private final Set<ConstructionAction> allyActions = EnumSet.noneOf(ConstructionAction.class);
    private final Map<ClanMember.MemberRank, Set<SettlerAction>> settlerPermissions =
            new EnumMap<>(ClanMember.MemberRank.class);
    /** Whether allies may open a camp's containers until a clan changes it. */
    @Getter
    private boolean allyContainers;
    private final Map<String, StructureNumbers> structures = new HashMap<>();

    @Inject
    public CampConfig(@NotNull Clans clans) {
        this.clans = clans;
        clans.getReloadables().add(this);
        reload();
    }

    @Override
    public void reload() {
        final ExtendedYamlConfiguration config = clans.getConfig("camps");
        chestCapacity = config.getInt("resources.chest-capacity", 250);
        claimLayers = config.getInt("construction.claim-layers", 3);

        deposits.clear();
        overflowItems.clear();
        for (ResourceKind kind : ResourceKind.values()) {
            final ConfigurationSection values = config.getConfigurationSection("deposit." + kind.id());
            if (values != null) {
                values.getKeys(false).forEach(item ->
                        deposits.put(item.toLowerCase(Locale.ROOT), new Deposit(kind, values.getInt(item))));
            }
            final Material overflow = Material.matchMaterial(config.getString("resources.overflow-item." + kind.id(), ""));
            if (overflow != null) {
                overflowItems.put(kind, overflow);
            }
        }

        structures.clear();
        final ConfigurationSection structureSection = config.getConfigurationSection("structures");
        if (structureSection != null) {
            for (String id : structureSection.getKeys(false)) {
                structures.put(id, readStructure(id, structureSection.getConfigurationSection(id)));
            }
        }

        permissions.clear();
        for (ClanMember.MemberRank rank : ClanMember.MemberRank.values()) {
            permissions.put(rank, actions(config.getStringList("permissions." + rank.name()), rank.name()));
        }
        allyActions.clear();
        allyActions.addAll(actions(config.getStringList("permissions.ALLY"), "ALLY"));
        allyContainers = config.getBoolean("permissions.ally-containers", false);

        settlerPermissions.clear();
        for (ClanMember.MemberRank rank : ClanMember.MemberRank.values()) {
            final Set<SettlerAction> actions = EnumSet.noneOf(SettlerAction.class);
            for (String action : config.getStringList("permissions.settlers." + rank.name())) {
                try {
                    actions.add(SettlerAction.valueOf(action.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    log.warn("Unknown settler permission '{}' for {}", action, rank).submit();
                }
            }
            settlerPermissions.put(rank, actions);
        }
    }

    private @NotNull Set<ConstructionAction> actions(@NotNull List<String> names, @NotNull String who) {
        final Set<ConstructionAction> actions = EnumSet.noneOf(ConstructionAction.class);
        for (String action : names) {
            try {
                actions.add(ConstructionAction.valueOf(action.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                log.warn("Unknown camp permission '{}' for {}", action, who).submit();
            }
        }
        return actions;
    }

    /** The numbers for structure {@code id}, or nothing if {@code camps.yml} does not list it. */
    public @NotNull Optional<StructureNumbers> structure(@NotNull String id) {
        return Optional.ofNullable(structures.get(id));
    }

    private @NotNull StructureNumbers readStructure(@NotNull String id, @NotNull ConfigurationSection section) {
        final List<StructureStage> stages = new ArrayList<>();
        for (Map<?, ?> stage : section.getMapList("stages")) {
            final Object schematic = stage.get("schematic");
            final Object seconds = stage.get("build-seconds");
            stages.add(new StructureStage(schematic == null ? id : schematic.toString(),
                    cost(stage.get("cost")), Duration.ofSeconds(seconds instanceof Number number ? number.longValue() : 0)));
        }
        if (stages.isEmpty()) {
            log.warn("Camp structure '{}' has no stages in camps.yml", id).submit();
            stages.add(new StructureStage(id, ResourceCost.NONE, Duration.ZERO));
        }

        final Material icon = Material.matchMaterial(section.getString("icon", "BRICKS"));
        return new StructureNumbers(List.copyOf(stages),
                cost(section.get("move.cost")), Duration.ofSeconds(section.getLong("move.seconds", 0)),
                cost(section.get("repair.cost")), Duration.ofSeconds(section.getLong("repair.seconds", 0)),
                section.getDouble("demolish-refund", 0), icon == null ? Material.BRICKS : icon);
    }

    /** Reads a {@code {wood: 10, stone: 5}} cost, from a config section or a plain map. */
    private static @NotNull ResourceCost cost(@Nullable Object raw) {
        final Map<String, Integer> amounts = new LinkedHashMap<>();
        if (raw instanceof ConfigurationSection section) {
            section.getKeys(false).forEach(resource -> amounts.put(resource, section.getInt(resource)));
        } else if (raw instanceof Map<?, ?> map) {
            map.forEach((resource, amount) -> {
                if (amount instanceof Number number) {
                    amounts.put(resource.toString(), number.intValue());
                }
            });
        }
        return ResourceCost.of(amounts);
    }

    /** What an item is worth when deposited, by its item key, if anything. */
    public @NotNull Optional<Deposit> depositValue(@NotNull String itemKey) {
        return Optional.ofNullable(deposits.get(itemKey.toLowerCase(Locale.ROOT)));
    }

    public @Nullable Material overflowItem(@NotNull ResourceKind kind) {
        return overflowItems.get(kind);
    }

    public @NotNull Map<ClanMember.MemberRank, Set<ConstructionAction>> defaultPermissions() {
        return Collections.unmodifiableMap(permissions);
    }

    public @NotNull Map<ClanMember.MemberRank, Set<SettlerAction>> defaultSettlerPermissions() {
        return Collections.unmodifiableMap(settlerPermissions);
    }

    /** What allies may do until a clan changes it. */
    public @NotNull Set<ConstructionAction> defaultAllyActions() {
        return Collections.unmodifiableSet(allyActions);
    }

    /** One structure's numbers. */
    @Value
    public static class StructureNumbers {
        List<StructureStage> stages;
        ResourceCost moveCost;
        Duration moveTime;
        ResourceCost repairCost;
        Duration repairTime;
        double demolishRefund;
        Material icon;
    }

    /** One item's worth: which resource, and how much of it. */
    @Value
    public static class Deposit {
        ResourceKind kind;
        int amount;
    }
}
