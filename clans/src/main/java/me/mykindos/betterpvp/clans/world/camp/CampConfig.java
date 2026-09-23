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
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
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

        permissions.clear();
        for (ClanMember.MemberRank rank : ClanMember.MemberRank.values()) {
            final Set<ConstructionAction> actions = EnumSet.noneOf(ConstructionAction.class);
            for (String action : config.getStringList("permissions." + rank.name())) {
                try {
                    actions.add(ConstructionAction.valueOf(action.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    log.warn("Unknown camp permission '{}' for {}", action, rank).submit();
                }
            }
            permissions.put(rank, actions);
        }
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

    /** One item's worth: which resource, and how much of it. */
    @Value
    public static class Deposit {
        ResourceKind kind;
        int amount;
    }
}
