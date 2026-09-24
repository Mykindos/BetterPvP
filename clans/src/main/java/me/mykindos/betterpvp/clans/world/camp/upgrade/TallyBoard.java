package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.resource.CampResources;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceChests;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceKind;
import me.mykindos.betterpvp.clans.world.camp.storage.CampChest;
import me.mykindos.betterpvp.clans.world.camp.storage.StorehouseChests;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Storehouse upgrade: a board showing each resource chest's share of the camp's balance. The balance is one number per
 * resource, so every resource chest holds an even share of it, the first chests taking any remainder.
 */
@BPvPListener
@Singleton
public class TallyBoard implements Listener {

    public static final String ID = "tally_board";

    private final CampStore store;
    private final ResourceChests resourceChests;
    private final CampResources resources;
    @Getter(AccessLevel.PACKAGE)
    private final StorehouseChests chests;

    @Inject
    public TallyBoard(@NotNull CampUpgrades upgrades, @NotNull CampStore store,
                      @NotNull ResourceChests resourceChests, @NotNull CampResources resources,
                      @NotNull StorehouseChests chests) {
        this.store = store;
        this.resourceChests = resourceChests;
        this.resources = resources;
        this.chests = chests;
        upgrades.declare(CampStructures.STOREHOUSE, ID, 1);
        upgrades.page(ID, (player, camp, structure, previous) -> new TallyBoardMenu(this, player, camp, previous)
                .show(player));
    }

    /** Every resource chest of camp {@code key} with its share, empty when {@code player} is not in that camp. */
    public @NotNull List<Share> tally(@NotNull Player player, @NotNull SiteKey key) {
        final Optional<ConstructionService.Worksite> here = chests.here(player, key);
        final Optional<Camp> camp = store.cached(key.getOwnerId());
        if (here.isEmpty() || camp.isEmpty()) {
            return List.of();
        }
        final List<CampChest> found = resourceChests.chests(here.get().getHolding(), here.get().getWorld());
        final Map<ResourceKind, int[]> split = new EnumMap<>(ResourceKind.class);
        for (ResourceKind kind : ResourceKind.values()) {
            split.put(kind, shares(camp.get().getResource(kind.id()), found.size()));
        }
        final int[] capacity = shares(resources.capacity(camp.get()), found.size());

        final List<Share> tally = new ArrayList<>(found.size());
        for (int i = 0; i < found.size(); i++) {
            final Map<ResourceKind, Integer> amounts = new EnumMap<>(ResourceKind.class);
            for (ResourceKind kind : ResourceKind.values()) {
                amounts.put(kind, split.get(kind)[i]);
            }
            tally.add(new Share(found.get(i), amounts, capacity[i]));
        }
        return tally;
    }

    /** {@code total} split across {@code chests} as evenly as whole numbers allow, the first chests taking the rest. */
    static int[] shares(int total, int chests) {
        final int[] shares = new int[Math.max(0, chests)];
        if (chests <= 0) {
            return shares;
        }
        final int each = Math.max(0, total) / chests;
        final int rest = Math.max(0, total) % chests;
        for (int i = 0; i < chests; i++) {
            shares[i] = each + (i < rest ? 1 : 0);
        }
        return shares;
    }

    /** One resource chest, what it holds of each resource and how much it can hold. */
    @Value
    public static class Share {
        CampChest chest;
        Map<ResourceKind, Integer> amounts;
        int capacity;

        public int total() {
            return amounts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }
}
