package me.mykindos.betterpvp.core.world.construction;

import lombok.Value;
import me.mykindos.betterpvp.core.world.schematic.LayerPlan;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The items in a structure's chests and barrels, kept on the {@link PlacedStructure} rather than in the world. A
 * structure's blocks are pasted again every time its world opens, so the record is what survives. Each container is
 * keyed by where it sits in the build, which a move or a rotation does not change.
 */
public final class StructureStorage {

    private static final Set<Material> CONTAINERS = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);

    private StructureStorage() {
    }

    /** One container in a placed build. */
    @Value
    public static class Slot {
        /** Where it sits in the build. */
        String key;
        /** The layer of the plan it goes down with. */
        int layer;
        int x;
        int y;
        int z;
    }

    /** Every container in {@code placement}, in layer order. */
    public static @NotNull List<Slot> slots(@NotNull SchematicPlacement placement, @NotNull LayerPlan plan) {
        final List<Slot> slots = new ArrayList<>();
        for (int layer = 0; layer < plan.size(); layer++) {
            for (Schematic.PlacedBlock block : plan.layer(layer)) {
                if (CONTAINERS.contains(block.getData().getMaterial())) {
                    final Schematic.PlacedBlock placed = placement.place(block);
                    slots.add(new Slot(key(block), layer, placed.getX(), placed.getY(), placed.getZ()));
                }
            }
        }
        return slots;
    }

    static @NotNull String key(@NotNull Schematic.PlacedBlock block) {
        return block.getX() + "," + block.getY() + "," + block.getZ();
    }

    /** Puts what the record holds for {@code slot} into the block standing there. */
    public static void restore(@NotNull World world, @NotNull PlacedStructure structure, @NotNull Slot slot) {
        final Inventory inventory = inventory(world, slot);
        final List<String> stored = structure.getStorage() == null ? null : structure.getStorage().get(slot.getKey());
        if (inventory == null || stored == null) {
            return;
        }
        final ItemStack[] contents = new ItemStack[inventory.getSize()];
        for (int i = 0; i < stored.size() && i < contents.length; i++) {
            contents[i] = decode(stored.get(i));
        }
        inventory.setContents(contents);
    }

    /**
     * Writes what the block standing at {@code slot} holds into the record.
     *
     * @return whether a container was there to read
     */
    public static boolean capture(@NotNull World world, @NotNull PlacedStructure structure, @NotNull Slot slot) {
        final Inventory inventory = inventory(world, slot);
        if (inventory == null) {
            return false;
        }
        final List<String> encoded = new ArrayList<>(inventory.getSize());
        boolean any = false;
        for (ItemStack item : inventory.getContents()) {
            final String value = encode(item);
            any |= value != null;
            encoded.add(value);
        }
        final Map<String, List<String>> storage = structure.getStorage() == null
                ? new HashMap<>() : new HashMap<>(structure.getStorage());
        if (any) {
            storage.put(slot.getKey(), encoded);
        } else {
            storage.remove(slot.getKey());
        }
        structure.setStorage(storage.isEmpty() ? null : storage);
        return true;
    }

    /** Closes the block at {@code slot} for anyone looking into it. */
    public static void close(@NotNull World world, @NotNull Slot slot) {
        final Inventory inventory = inventory(world, slot);
        if (inventory != null) {
            new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
        }
    }

    /** Empties the block at {@code slot}, so taking it out of the world drops nothing. */
    public static void empty(@NotNull World world, @NotNull Slot slot) {
        final Inventory inventory = inventory(world, slot);
        if (inventory != null) {
            inventory.clear();
        }
    }

    /**
     * Takes out of the record everything kept for a container {@code slots} no longer has, as a changed build can lose
     * one, and puts it into the containers that remain. Whatever does not fit is dropped at {@code at}.
     *
     * @return whether the record changed
     */
    public static boolean settle(@NotNull World world, @NotNull PlacedStructure structure, @NotNull List<Slot> slots,
                                 @NotNull Location at) {
        final Map<String, List<String>> storage = structure.getStorage();
        if (storage == null) {
            return false;
        }
        final Set<String> present = new HashSet<>();
        slots.forEach(slot -> present.add(slot.getKey()));
        final List<ItemStack> orphans = new ArrayList<>();
        final Map<String, List<String>> kept = new HashMap<>();
        storage.forEach((key, items) -> {
            if (present.contains(key)) {
                kept.put(key, items);
            } else {
                items.stream().map(StructureStorage::decode).filter(Objects::nonNull).forEach(orphans::add);
            }
        });
        if (orphans.isEmpty()) {
            return false;
        }

        for (Slot slot : slots) {
            final Inventory inventory = inventory(world, slot);
            if (inventory != null && !orphans.isEmpty()) {
                final Map<Integer, ItemStack> left = inventory.addItem(orphans.toArray(new ItemStack[0]));
                orphans.clear();
                orphans.addAll(left.values());
            }
        }
        orphans.forEach(item -> world.dropItemNaturally(at, item));
        structure.setStorage(kept.isEmpty() ? null : kept);
        slots.forEach(slot -> capture(world, structure, slot));
        return true;
    }

    /** Drops everything the record holds at {@code at} and forgets it. */
    public static void drop(@NotNull PlacedStructure structure, @NotNull Location at) {
        final Map<String, List<String>> storage = structure.getStorage();
        if (storage == null || at.getWorld() == null) {
            return;
        }
        storage.values().forEach(items -> items.stream()
                .map(StructureStorage::decode)
                .filter(Objects::nonNull)
                .forEach(item -> at.getWorld().dropItemNaturally(at, item)));
        structure.setStorage(null);
    }

    private static @Nullable Inventory inventory(@NotNull World world, @NotNull Slot slot) {
        if (!world.isChunkLoaded(slot.getX() >> 4, slot.getZ() >> 4)) {
            return null;
        }
        final Block block = world.getBlockAt(slot.getX(), slot.getY(), slot.getZ());
        if (!CONTAINERS.contains(block.getType())) {
            return null;
        }
        // A chest's own half, so each block of a double chest keeps its own contents.
        if (block.getState(false) instanceof Chest chest) {
            return chest.getBlockInventory();
        }
        return block.getState(false) instanceof Container container ? container.getInventory() : null;
    }

    private static @Nullable String encode(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    private static @Nullable ItemStack decode(@Nullable String value) {
        return value == null ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(value));
    }
}
