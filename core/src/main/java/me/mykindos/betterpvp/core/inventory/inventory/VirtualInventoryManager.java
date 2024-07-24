package me.mykindos.betterpvp.core.inventory.inventory;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.InvUI;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Automatically reads and writes {@link VirtualInventory VirtualInventories} to files when the server starts and stops.
 */
@Singleton
@CustomLog
public class VirtualInventoryManager {

    private static final File SAVE_DIR = new File("plugins/Core/VirtualInventory/" + InvUI.getInstance().getPlugin().getName() + "/");

    private static VirtualInventoryManager instance;

    private final Map<UUID, VirtualInventory> inventories = new HashMap<>();

    private VirtualInventoryManager() {
        InvUI.getInstance().addDisableHandler(this::serializeAll);
        deserializeAll();
    }

    public static VirtualInventoryManager getInstance() {
        return instance == null ? instance = new VirtualInventoryManager() : instance;
    }

    public VirtualInventory createNew(@NotNull UUID uuid, int size) {
        if (inventories.containsKey(uuid))
            throw new IllegalArgumentException("A VirtualInventory with that UUID already exists");

        VirtualInventory inventory = new VirtualInventory(uuid, size);
        inventories.put(uuid, inventory);

        return inventory;
    }

    public VirtualInventory createNew(@NotNull UUID uuid, int size, ItemStack[] items, int[] stackSizes) {
        if (inventories.containsKey(uuid))
            throw new IllegalArgumentException("A Virtual Inventory with that UUID already exists");

        VirtualInventory inventory = new VirtualInventory(uuid, size, items, stackSizes);
        inventories.put(uuid, inventory);

        return inventory;
    }

    public VirtualInventory getByUuid(@NotNull UUID uuid) {
        return inventories.get(uuid);
    }

    public VirtualInventory getOrCreate(UUID uuid, int size) {
        VirtualInventory inventory = getByUuid(uuid);
        return inventory == null ? createNew(uuid, size) : inventory;
    }

    public VirtualInventory getOrCreate(UUID uuid, int size, ItemStack[] items, int[] stackSizes) {
        VirtualInventory inventory = getByUuid(uuid);
        return inventory == null ? createNew(uuid, size, items, stackSizes) : inventory;
    }

    public List<VirtualInventory> getAllInventories() {
        return new ArrayList<>(inventories.values());
    }

    public void remove(VirtualInventory inventory) {
        inventories.remove(inventory.getUuid(), inventory);
        if (!getSaveFile(inventory).delete()) {
            log.warn("Failed to delete save file for VirtualInventory with UUID {}", inventory.getUuid()).submit();
        }
    }

    private void deserializeAll() {
        if (!SAVE_DIR.exists())
            return;

        for (File file : SAVE_DIR.listFiles()) {
            if (!file.getName().endsWith(".vi2"))
                return;

            try (FileInputStream in = new FileInputStream(file)) {
                VirtualInventory inventory = VirtualInventory.deserialize(in);
                inventories.put(inventory.getUuid(), inventory);
            } catch (IOException e) {
                log.error("Failed to deserialize a VirtualInventory from file " + file.getPath(), e).submit();

            }
        }
    }

    private void serializeAll() {
        if (inventories.isEmpty())
            return;

        if(!SAVE_DIR.mkdirs()) {
            log.warn("Failed to create save directory for VirtualInventories").submit();
        }

        for (VirtualInventory inventory : inventories.values()) {
            File file = getSaveFile(inventory);
            try (FileOutputStream out = new FileOutputStream(file)) {
                inventory.serialize(out);
            } catch (IOException e) {
                log.error("Failed to serialize a VirtualInventory to file " + file.getPath(), e).submit();
            }
        }
    }

    private File getSaveFile(VirtualInventory inventory) {
        return new File(SAVE_DIR, inventory.getUuid() + ".vi2");
    }

}
