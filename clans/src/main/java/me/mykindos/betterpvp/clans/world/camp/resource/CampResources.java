package me.mykindos.betterpvp.clans.world.camp.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.settler.CampTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.CampWideTraits;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.ResourceLedger;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A camp's Wood, Stone and Iron. Resources are a balance kept on the camp's record, not items, so they cannot be
 * misplaced and are read the same way whether the camp's world is open or not.
 */
@Singleton
public class CampResources implements ResourceLedger {

    private final CampStore store;
    private final ResourceChests chests;
    private final CampWideTraits campWide;

    @Inject
    public CampResources(@NotNull CampStore store, @NotNull ResourceChests chests, @NotNull CampWideTraits campWide) {
        this.store = store;
        this.chests = chests;
        this.campWide = campWide;
    }

    @Override
    public boolean canAfford(@NotNull SiteKey site, @NotNull ResourceCost cost) {
        return store.cached(site.getOwnerId())
                .map(camp -> cost.getAmounts().entrySet().stream()
                        .allMatch(entry -> camp.getResource(entry.getKey()) >= entry.getValue()))
                .orElse(false);
    }

    @Override
    public void spend(@NotNull SiteKey site, @NotNull ResourceCost cost) {
        change(site, cost, -1);
    }

    @Override
    public void refund(@NotNull SiteKey site, @NotNull ResourceCost cost) {
        change(site, cost, 1);
    }

    /** Everything the camp holds, across all resources. */
    public int total(@NotNull Camp camp) {
        return camp.getResources().values().stream().mapToInt(Integer::intValue).sum();
    }

    /** What the camp's resource chests hold together, more with a Quartermaster. */
    public int capacity(@NotNull Camp camp) {
        final double quartermaster = campWide.best(camp.getRoster(), CampTraits.QUARTERMASTER, "capacity", 0.05);
        return (int) Math.floor(chests.capacity(camp.getHolding()) * (1 + quartermaster));
    }

    /** Adds {@code amounts} to the camp's balance, with no checks. */
    public void add(long clanId, @NotNull Camp camp, @NotNull Map<ResourceKind, Integer> amounts) {
        amounts.forEach((kind, amount) -> camp.getResources().merge(kind.id(), amount, Integer::sum));
        store.changed(clanId);
    }

    private void change(@NotNull SiteKey site, @NotNull ResourceCost cost, int sign) {
        store.cached(site.getOwnerId()).ifPresent(camp -> {
            cost.getAmounts().forEach((resource, amount) ->
                    camp.getResources().merge(resource, sign * amount, Integer::sum));
            camp.getResources().replaceAll((resource, amount) -> Math.max(0, amount));
            store.changed(site.getOwnerId());
        });
    }
}
