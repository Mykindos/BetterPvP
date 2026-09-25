package me.mykindos.betterpvp.clans.world.camp.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.resource.CampResources;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.clans.world.camp.upgrade.BuildQueue;
import me.mykindos.betterpvp.clans.world.camp.upgrade.RushOrder;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.blueprint.BlueprintSessions;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Opens the menus for a camp's placed structures: the list of them, and for each one what can be done to it. The list
 * reads from the camp record so it works from anywhere, while acting needs the player to stand in the camp.
 */
@Singleton
@Getter(AccessLevel.PACKAGE)
public class StructureMenus {

    private final ConstructionService construction;
    private final StructureCatalogue catalogue;
    private final CampStore store;
    private final CampPermissions permissions;
    private final CampResources resources;
    private final BlueprintSessions blueprints;
    private final BuildQueue buildQueue;
    private final RushOrder rushOrder;

    @Inject
    public StructureMenus(@NotNull ConstructionService construction, @NotNull StructureCatalogue catalogue,
                          @NotNull CampStore store, @NotNull CampPermissions permissions,
                          @NotNull CampResources resources, @NotNull BlueprintSessions blueprints,
                          @NotNull BuildQueue buildQueue, @NotNull RushOrder rushOrder) {
        this.construction = construction;
        this.catalogue = catalogue;
        this.store = store;
        this.permissions = permissions;
        this.resources = resources;
        this.blueprints = blueprints;
        this.buildQueue = buildQueue;
        this.rushOrder = rushOrder;
    }

    /** Every structure camp {@code camp} has. Back leads to {@code previous}. */
    public void openList(@NotNull Player player, @NotNull SiteKey camp, @Nullable Windowed previous) {
        new PlacedStructuresMenu(this, camp, previous).show(player);
    }

    /** What can be done to structure {@code id} of camp {@code camp}, with Back leading to {@code returnTo}. */
    public void openActions(@NotNull Player player, @NotNull SiteKey camp, @NotNull UUID id,
                            @NotNull Windowed returnTo) {
        new StructureActionsMenu(this, player, camp, id, null, null, returnTo).show(player);
    }

    @NotNull Optional<Holding> holding(@NotNull SiteKey camp) {
        return store.cached(camp.getOwnerId()).map(Camp::getHolding);
    }

    /** The camp's worksite, only if {@code player} stands in it. */
    @NotNull Optional<ConstructionService.Worksite> worksite(@NotNull Player player, @NotNull SiteKey camp) {
        return construction.worksite(player.getWorld()).filter(worksite -> worksite.getKey().equals(camp));
    }

    @NotNull Optional<CampStructure> type(@NotNull String id) {
        return catalogue.find(id).filter(CampStructure.class::isInstance).map(CampStructure.class::cast);
    }

    void tell(@NotNull Player player, @NotNull Component message) {
        UtilMessage.plain(player, message);
    }
}
