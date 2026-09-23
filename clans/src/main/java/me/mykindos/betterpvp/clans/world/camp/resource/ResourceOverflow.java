package me.mykindos.betterpvp.clans.world.camp.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureContents;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/**
 * The resource chests a demolished structure had are gone with it, so whatever the camp now holds past what its
 * remaining chests take is dropped where the structure stood, largest balance first, as items.
 */
@Singleton
public class ResourceOverflow implements StructureContents {

    private final CampStore store;
    private final CampResources resources;
    private final CampConfig config;

    @Inject
    public ResourceOverflow(@NotNull CampStore store, @NotNull CampResources resources, @NotNull CampConfig config) {
        this.store = store;
        this.resources = resources;
        this.config = config;
    }

    @Override
    public void drop(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Location at) {
        store.cached(site.getOwnerId()).ifPresent(camp -> drop(site.getOwnerId(), camp, at));
    }

    private void drop(long clanId, @NotNull Camp camp, @NotNull Location at) {
        int excess = resources.total(camp) - resources.capacity(camp);
        if (excess <= 0) {
            return;
        }

        final List<ResourceKind> largestFirst = List.of(ResourceKind.values()).stream()
                .sorted(Comparator.comparingInt((ResourceKind kind) -> camp.getResource(kind.id())).reversed())
                .toList();
        for (ResourceKind kind : largestFirst) {
            final int taken = Math.min(excess, camp.getResource(kind.id()));
            if (taken <= 0) {
                continue;
            }
            camp.getResources().put(kind.id(), camp.getResource(kind.id()) - taken);
            excess -= taken;
            dropItems(kind, taken, at);
            if (excess <= 0) {
                break;
            }
        }
        store.changed(clanId);
    }

    private void dropItems(@NotNull ResourceKind kind, int amount, @NotNull Location at) {
        final Material material = config.overflowItem(kind);
        if (material == null) {
            return;
        }
        int left = amount;
        while (left > 0) {
            final int stack = Math.min(left, material.getMaxStackSize());
            at.getWorld().dropItemNaturally(at, new ItemStack(material, stack));
            left -= stack;
        }
    }
}
