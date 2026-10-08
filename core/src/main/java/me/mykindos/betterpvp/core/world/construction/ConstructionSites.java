package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** The {@link ConstructionSite} registered for each site id, and the worksite a world or site key belongs to. */
@Singleton
public class ConstructionSites {

    private final SiteInstances instances;
    private final StructureCatalogue catalogue;
    private final Map<String, ConstructionSite> sites = new HashMap<>();

    @Inject
    public ConstructionSites(@NotNull SiteInstances instances, @NotNull StructureCatalogue catalogue) {
        this.instances = instances;
        this.catalogue = catalogue;
    }

    /** Makes construction possible on every instance of the site {@code siteId}. */
    public void register(@NotNull String siteId, @NotNull ConstructionSite site) {
        sites.put(siteId, site);
    }

    /** Whether {@code world} is an instance of any site, registered for construction or not. */
    public boolean isSite(@NotNull World world) {
        return instances.byWorld(world.getName()).isPresent();
    }

    /** The holding a world belongs to, if construction happens there and its record is loaded. */
    public @NotNull Optional<Worksite> worksite(@NotNull World world) {
        return instances.byWorld(world.getName()).map(SiteInstance::getKey).flatMap(this::holding)
                .map(at -> new Worksite(at.getKey(), at.getSite(), at.getHolding(), world));
    }

    /** The holding of {@code key}, if construction happens there and its record is loaded, loaded world or not. */
    @NotNull Optional<SiteHolding> holding(@NotNull SiteKey key) {
        return Optional.ofNullable(sites.get(key.getSiteId()))
                .flatMap(site -> site.holding(key).map(holding -> new SiteHolding(key, site, holding)));
    }

    /** Whether {@code player} may use the features of {@code structure} on {@code site}: anyone if it is public. */
    public boolean canUse(@NotNull Player player, @NotNull SiteKey site, @NotNull PlacedStructure structure) {
        final boolean open = catalogue.find(structure.getType())
                .map(type -> type.getFlags().isPublicUse())
                .orElse(false);
        if (open) {
            return true;
        }
        final ConstructionSite owner = sites.get(site.getSiteId());
        return owner != null && owner.isMember(player, site);
    }

    /** What demolishing {@code structure} on {@code site} gives back. */
    public @NotNull ResourceCost demolishRefund(@NotNull SiteKey site, @NotNull PlacedStructure structure,
                                                @NotNull StructureType type) {
        final double share = Optional.ofNullable(sites.get(site.getSiteId()))
                .map(owner -> owner.demolishRefund(site, structure, type))
                .orElseGet(() -> type.getFlags().getDemolishRefund());
        return type.costUpTo(structure.getStage()).share(share);
    }
}
