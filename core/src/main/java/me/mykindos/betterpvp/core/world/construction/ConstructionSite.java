package me.mykindos.betterpvp.core.world.construction;

import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Everything construction needs from the module that owns a kind of site: where its holdings are kept, what pays for
 * building, who may do what, and any rules of its own. Registered per site id with the {@link ConstructionService}, so
 * core never learns what the owner of a site is.
 */
public interface ConstructionSite {

    /** The holding for {@code site}, or empty if its record is not loaded. */
    @NotNull Optional<Holding> holding(@NotNull SiteKey site);

    /** Called after every change to a holding, so its record can be written down. */
    void changed(@NotNull SiteKey site);

    @NotNull ResourceLedger ledger();

    /** Whether {@code player} may take {@code action} on {@code site}. */
    boolean allows(@NotNull Player player, @NotNull SiteKey site, @NotNull ConstructionAction action);

    /**
     * Why {@code type} cannot be taken to {@code version} here, beyond the structures it requires, or empty if nothing
     * stands in the way. This is where a site's own gates go, such as a tier needing a particular hall.
     */
    default @NotNull Optional<Component> blocked(@NotNull SiteKey site, @NotNull Holding holding,
                                                 @NotNull StructureType type, int version) {
        return Optional.empty();
    }

    /** How many of a structure's top layers are held back until it is claimed, then animate in. */
    default int claimLayers() {
        return 3;
    }

    default @NotNull List<JobRule> jobRules() {
        return List.of();
    }

    default @NotNull List<StructureContents> contents() {
        return List.of();
    }
}
