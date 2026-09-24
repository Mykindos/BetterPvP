package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampRespawn;
import me.mykindos.betterpvp.clans.world.camp.CampRespawnEvent;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.SiteLandings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Barracks upgrade: a second respawn spot, the {@code respawn} point in the upgrade's piece. Each member picks which
 * door they respawn at, and the choice is kept on the camp record. While the upgrade works, members who picked the
 * second door respawn there, landing on {@link #LANDING} when they travel from another server. Without the upgrade or
 * the point they respawn at the Barracks as usual.
 */
@BPvPListener
@Singleton
public class SecondDoor implements Listener {

    public static final String ID = "second_door";
    /** The landing members who picked the second door travel to. */
    public static final String LANDING = "barracks_second_door";

    private final CampStore store;
    private final CampUpgrades upgrades;
    private final ConstructionService construction;
    private final StructureCatalogue catalogue;
    private final StructureShapes shapes;

    @Inject
    public SecondDoor(@NotNull CampStore store, @NotNull CampUpgrades upgrades,
                      @NotNull ConstructionService construction, @NotNull StructureCatalogue catalogue,
                      @NotNull StructureShapes shapes, @NotNull SiteLandings landings, @NotNull CampRespawn respawn) {
        this.store = store;
        this.upgrades = upgrades;
        this.construction = construction;
        this.catalogue = catalogue;
        this.shapes = shapes;
        upgrades.declare(CampStructures.BARRACKS, ID, 2);
        upgrades.page(ID, (player, camp, structure, previous) ->
                new SecondDoorMenu(this, player, camp, previous).show(player));
        landings.register(Camps.SITE_ID, LANDING, world -> spot(world).or(() -> respawn.barracks(world)));
    }

    @EventHandler
    public void onCampRespawn(@NotNull CampRespawnEvent event) {
        if (!picked(event.getCamp(), event.getPlayer().getUniqueId())) {
            return;
        }
        event.setLanding(LANDING);
        final World world = event.getWorld();
        if (world != null) {
            spot(world).ifPresent(event::setSpot);
        }
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.BARRACKS, ID);
    }

    /** Whether {@code member} picked the second door in camp {@code key}. */
    public boolean picked(@NotNull SiteKey key, @NotNull UUID member) {
        return store.cached(key.getOwnerId()).map(camp -> camp.getSecondDoor().contains(member)).orElse(false);
    }

    /**
     * Makes {@code member} respawn at the second door, or at the Barracks' own spot.
     *
     * @return the translation key of why it was refused, or null once done
     */
    public @Nullable String pick(@NotNull SiteKey key, @NotNull UUID member, boolean second) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null) {
            return "core.settler.not_loaded";
        }
        if (!isActive(key)) {
            return "clans.camp.upgrade.second_door.inactive";
        }
        final boolean changed = second ? camp.getSecondDoor().add(member) : camp.getSecondDoor().remove(member);
        if (changed) {
            store.changed(key.getOwnerId());
        }
        return null;
    }

    /** The second door's spot in {@code world}, while its Barracks has the upgrade working. */
    public @NotNull Optional<Location> spot(@NotNull World world) {
        return construction.worksite(world)
                .filter(worksite -> isActive(worksite.getKey()))
                .flatMap(worksite -> worksite.getHolding().ofType(CampStructures.BARRACKS).stream().findFirst())
                .flatMap(barracks -> catalogue.find(CampStructures.BARRACKS)
                        .flatMap(type -> type.getUpgrades().stream()
                                .filter(upgrade -> upgrade.getId().equals(ID))
                                .findFirst())
                        .flatMap(upgrade -> shapes.pieceOf(world, barracks, upgrade)))
                .flatMap(piece -> {
                    for (Region marker : piece.markers()) {
                        if (CampRespawn.MARKER.equalsIgnoreCase(marker.getName()) && marker instanceof PointRegion point) {
                            return Optional.of(point.getLocation().clone());
                        }
                    }
                    return Optional.empty();
                });
    }
}
