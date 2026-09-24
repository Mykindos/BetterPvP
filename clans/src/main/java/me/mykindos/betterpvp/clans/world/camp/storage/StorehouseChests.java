package me.mykindos.betterpvp.clans.world.camp.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceChests;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureStorage;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A camp's item chests: chests in a Storehouse's build carrying a Mapper point named {@code item_chest}. They count by
 * the same rule as resource chests, and their contents are the Storehouse's own storage, so whatever is put into one
 * is written back to the structure's record.
 */
@Singleton
public class StorehouseChests {

    public static final String MARKER = "item_chest";

    private final ChestPoints points;
    private final ConstructionService construction;
    private final CampStructures structures;

    @Inject
    public StorehouseChests(@NotNull ChestPoints points, @NotNull ConstructionService construction,
                            @NotNull CampStructures structures) {
        this.points = points;
        this.construction = construction;
        this.structures = structures;
    }

    /** Every item chest across the Storehouses of {@code holding} that count, Storehouse by Storehouse. */
    public @NotNull List<CampChest> chests(@NotNull Holding holding, @NotNull World world) {
        final List<CampChest> chests = new ArrayList<>();
        for (PlacedStructure structure : holding.ofType(CampStructures.STOREHOUSE)) {
            if (ResourceChests.counts(structure)) {
                chests.addAll(points.find(world, structure, MARKER));
            }
        }
        return chests;
    }

    /**
     * The inventory of {@code chest}'s own block, if it is standing as a container in a loaded chunk. A double chest
     * gives the half the point sits on, as each half is kept on its own.
     */
    public @NotNull Optional<Inventory> inventory(@NotNull World world, @NotNull CampChest chest) {
        if (chest.getSlot() == null || !world.isChunkLoaded(chest.getX() >> 4, chest.getZ() >> 4)) {
            return Optional.empty();
        }
        final Block block = chest.block(world);
        if (block.getState(false) instanceof Chest found) {
            return Optional.of(found.getBlockInventory());
        }
        return block.getState(false) instanceof Container container
                ? Optional.of(container.getInventory()) : Optional.empty();
    }

    /** What players see a chest as: its structure's stage name and its number among that structure's marked chests. */
    public @NotNull Component name(@NotNull CampChest chest) {
        final PlacedStructure structure = chest.getStructure();
        final Component stage = structures.find(structure.getType())
                .map(type -> type.stageName(structure.getStage()))
                .orElseGet(() -> Component.text(structure.getType()));
        return Translations.component("clans.camp.storage.chest", stage, Component.text(chest.getIndex()));
    }

    public static @NotNull Component position(@NotNull CampChest chest) {
        return Translations.component("clans.camp.storage.position", Component.text(chest.getX()),
                Component.text(chest.getY()), Component.text(chest.getZ()));
    }

    /** The worksite {@code player} stands in, if it is camp {@code key}'s. */
    public @NotNull Optional<ConstructionService.Worksite> here(@NotNull Player player, @NotNull SiteKey key) {
        return construction.worksite(player.getWorld()).filter(worksite -> worksite.getKey().equals(key));
    }

    /** Writes what {@code chest} holds now into its Storehouse's record. */
    public void save(@NotNull World world, @NotNull CampChest chest) {
        if (chest.getSlot() != null && StructureStorage.capture(world, chest.getStructure(), chest.getSlot())) {
            construction.worksite(world).ifPresent(worksite -> worksite.getSite().changed(worksite.getKey()));
        }
    }
}
