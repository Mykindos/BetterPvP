package me.mykindos.betterpvp.core.utilities;

import com.mojang.authlib.GameProfile;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@CustomLog
public class UtilInventory {

    public static boolean isPlayerInventory(Player player, int containerId) {
        return containerId == -2 || ((CraftPlayer) player).getHandle().inventoryMenu.containerId == containerId;
    }

    public static boolean contains(Player player, Material item, int required) {

        for (int i : player.getInventory().all(item).keySet()) {
            if (required <= 0) {
                return true;
            }

            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && stack.getAmount() > 0) {
                required -= stack.getAmount();
            }
        }

        return required <= 0;
    }

    public static boolean contains(Player player, Material item, int required, int customModelData) {
        for (int i : player.getInventory().all(item).keySet()) {
            if (required <= 0) {
                return true;
            }

            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && stack.getAmount() > 0) {
                ItemMeta itemMeta = stack.getItemMeta();
                if (itemMeta == null) continue;
                if (itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == customModelData) {
                    required -= stack.getAmount();
                }

            }
        }

        return required <= 0;
    }

    public static boolean contains(Player player, String namespacedKey, int required) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta == null) continue;

            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            if (pdc.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING)) {
                String key = pdc.get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING);
                if (key != null) {
                    if (key.equalsIgnoreCase(namespacedKey)) {
                        count += item.getAmount();
                    }
                }
            }

        }

        return count >= required || player.getGameMode() == GameMode.CREATIVE;
    }

    public static void remove(Player player, String namespacedKey, int amount) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        int amountToRemove = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta == null) continue;

            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            if (pdc.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING)) {
                String key = pdc.get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING);
                if (key != null) {
                    if (key.equalsIgnoreCase(namespacedKey)) {
                        if (item.getAmount() > amountToRemove) {
                            item.setAmount(item.getAmount() - amountToRemove);
                            return;
                        } else {
                            amountToRemove -= item.getAmount();
                            item.setAmount(0);
                        }
                    }
                }
            }

            if (amountToRemove <= 0) return;
        }
    }

    public static boolean remove(Player player, ItemStack itemStack) {

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta == null) continue;

            ItemMeta stackMeta = itemStack.getItemMeta();
            if(stackMeta != null) {
                if(!itemMeta.equals(stackMeta)) continue;
            }

            if(item.getType() != itemStack.getType()) continue;
            if(item.getAmount() != itemStack.getAmount()) continue;

            item.setAmount(0);
            return true;

        }

        return false;
    }

    private static boolean removeFromHand(Player player, ItemStack hand, Material item, int toRemove) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        if (hand.getType() == item) {
            if (hand.getAmount() > toRemove) {
                hand.setAmount(hand.getAmount() - toRemove);
            } else {
                hand.setAmount(0);
            }
            return true;
        }
        return false;
    }

    public static boolean remove(Player player, Material item, int toRemove) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;

        if (removeFromHand(player, player.getInventory().getItemInMainHand(), item, toRemove)) return true;
        if (removeFromHand(player, player.getInventory().getItemInOffHand(), item, toRemove)) return true;

        if (contains(player, item, toRemove)) {
            Map<Integer, ? extends ItemStack> allItems = player.getInventory().all(item);
            for (Map.Entry<Integer, ? extends ItemStack> entry : allItems.entrySet()) {
                ItemStack stack = entry.getValue();
                if (stack.getAmount() > toRemove) {
                    stack.setAmount(stack.getAmount() - toRemove);
                    return true;
                } else {
                    player.getInventory().setItem(entry.getKey(), null);
                    toRemove -= stack.getAmount();
                }
            }
        }
        return false;
    }

    public static int getCount(ItemStack[] contents, Predicate<ItemStack> matches) {
        int count = 0;
        for (ItemStack item : contents) {
            if (item != null && matches.test(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Saves the CraftPlayerInventory as the players inventory
     * in the playerdatafolder
     * @param id the UUID of the player
     * @param inventory the inventory of the player to save
     */
    public static void saveOfflineInventory(UUID id, CraftInventoryPlayer inventory) {
        //get the player's current data
        CompoundTag compound = UtilNBT.getPlayerData(id).orElseThrow();
        //overwrite the Inventory data with the modified inventory
        compound.put("Inventory", inventory.getInventory().save(new ListTag()));
        //save the players data
        UtilNBT.savePlayerData(id, compound);
    }

    /**
     * Gets the offline inventory of a player with the specified name and id
     * from their playerdata.dat file
     * @param name the name of the player
     * @param id the UUID of the player
     * @return the bukkit CraftInventoryPlayer inventory that belongs to this player
     */
    public static CraftInventoryPlayer getOfflineInventory(String name, UUID id) {
        //in order to access an offline players inventory, we need to load it
        //trying to do this custom didnt really work
        //so instead we just recreate how inventories are loaded
        //by using those exact methods

        //get data from the player.dat file
        CompoundTag compound = UtilNBT.getPlayerData(id).orElseThrow();

        //get the inventory nbt data
        ListTag nbttaglist = compound.getList("Inventory", 10);

        //in order to load the inventory, we need a ServerPlayer, server players require this
        //data. Defaults are fine, this is only used to load the inventory
        MinecraftServer server = MinecraftServer.getServer();
        ServerLevel serverLevel = server.getLevel(Level.OVERWORLD);
        GameProfile gameProfile = new GameProfile(id, name);
        ClientInformation clientOptions= ClientInformation.createDefault();

        ServerPlayer serverPlayer = new ServerPlayer(server, serverLevel, gameProfile, clientOptions);

        //create Minecraft Inventory
        Inventory inventory = new net.minecraft.world.entity.player.Inventory(serverPlayer);

        //load that inventory from the NBT
        inventory.load(nbttaglist);

        //return the BukkitPlayerInventory (same one you get from player#getInventory())
        return new CraftInventoryPlayer(inventory);

    }
}
