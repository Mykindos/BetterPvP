package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.resource.CampResources;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceOverflow;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.ConstructionSite;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceLedger;
import me.mykindos.betterpvp.core.world.construction.StructureContents;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Everything construction needs from a camp: its holding is kept on the camp record, it pays in Wood, Stone and Iron,
 * each rank does what the clan allows, and higher tiers wait on the Great Hall.
 */
@Singleton
public class CampConstruction implements ConstructionSite {

    /** The structure whose stage decides which tiers can be built. */
    public static final String GREAT_HALL = "great_hall";

    private final CampStore store;
    private final CampResources resources;
    private final CampPermissions permissions;
    private final CampConfig config;
    private final ResourceOverflow overflow;

    @Inject
    public CampConstruction(@NotNull CampStore store, @NotNull CampResources resources,
                            @NotNull CampPermissions permissions, @NotNull CampConfig config,
                            @NotNull ResourceOverflow overflow, @NotNull ConstructionService service) {
        this.store = store;
        this.resources = resources;
        this.permissions = permissions;
        this.config = config;
        this.overflow = overflow;
        service.register(Camps.SITE_ID, this);
    }

    @Override
    public @NotNull Optional<Holding> holding(@NotNull SiteKey site) {
        return store.cached(site.getOwnerId()).map(Camp::getHolding);
    }

    @Override
    public void changed(@NotNull SiteKey site) {
        store.changed(site.getOwnerId());
    }

    @Override
    public @NotNull ResourceLedger ledger() {
        return resources;
    }

    @Override
    public boolean allows(@NotNull Player player, @NotNull SiteKey site, @NotNull ConstructionAction action) {
        return permissions.allows(player, site.getOwnerId(), action);
    }

    /** Tier N needs a Great Hall at its Nth stage. Stages count from zero, tiers from one. */
    @Override
    public @NotNull Optional<Component> blocked(@NotNull SiteKey site, @NotNull Holding holding,
                                                @NotNull StructureType type, int stage) {
        if (type.getTier() <= 1 || type.getId().equals(GREAT_HALL)) {
            return Optional.empty();
        }
        final int hallStage = holding.ofType(GREAT_HALL).stream()
                .mapToInt(PlacedStructure::getStage)
                .max()
                .orElse(-1);
        if (hallStage + 1 >= type.getTier()) {
            return Optional.empty();
        }
        return Optional.of(Translations.component("clans.camp.construction.needs_hall",
                Component.text(type.getTier())).color(NamedTextColor.RED));
    }

    @Override
    public int claimLayers() {
        return config.getClaimLayers();
    }

    @Override
    public @NotNull List<StructureContents> contents() {
        return List.of(overflow);
    }
}
