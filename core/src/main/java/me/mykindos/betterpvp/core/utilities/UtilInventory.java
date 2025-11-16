package me.mykindos.betterpvp.core.utilities;

import com.mojang.authlib.GameProfile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    public static void remove(Player player, BaseItem baseItem, int amount) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemFactory itemFactory = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ItemFactory.class);
        int amountToRemove = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;

            final ItemInstance content = itemFactory.fromItemStack(item).orElseThrow();
            if (content.getBaseItem().equals(baseItem)) {
                if (item.getAmount() > amountToRemove) {
                    item.setAmount(item.getAmount() - amountToRemove);
                    return;
                } else {
                    amountToRemove -= item.getAmount();
                    item.setAmount(0);
                }
            }

            if (amountToRemove <= 0) {
                return;
            }
        }
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

    public static void consumeHand(Player player) {
        final PlayerInventory inventory = player.getInventory();
        final ItemStack item = player.getEquipment().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        item.subtract();
        inventory.setItemInMainHand(item);
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

    /**
     * Removes up to the specified amount from a hand.
     * @param player the player to remove from
     * @param hand the items stack for the specified hand
     * @param item the material to remove
     * @param toRemove how many to remove
     * @return the number actually removed
     */
    private static int removeFromHand(Player player, ItemStack hand, Material item, int toRemove) {
        if (player.getGameMode() == GameMode.CREATIVE) return toRemove;
        if (hand.getType() == item) {
            if (hand.getAmount() > toRemove) {
                hand.setAmount(hand.getAmount() - toRemove);
                return toRemove;
            }

            int amountRemoved = hand.getAmount();
            hand.setAmount(0);
            return amountRemoved;
        }
        return 0;
    }

    /**
     * Removes up to the amount of items from the player
     * @param player the player
     * @param item the material to remove
     * @param toRemove the amount to remove
     * @return whether all items were removed
     */
    public static boolean remove(Player player, Material item, int toRemove) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;

        toRemove -= removeFromHand(player, player.getInventory().getItemInMainHand(), item, toRemove);
        if (toRemove == 0) {
            return true;
        }
        toRemove -= removeFromHand(player, player.getInventory().getItemInOffHand(), item, toRemove);
        if (toRemove == 0) {
            return true;
        }

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

                if (toRemove == 0) {
                    return true;
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

        //Use the new ValueOutput API to save inventory
        TagValueOutput output = TagValueOutput.createWrappingGlobal(ProblemReporter.DISCARDING, compound);
        ValueOutput.TypedOutputList<ItemStackWithSlot> invOutput = output.list("Inventory", ItemStackWithSlot.CODEC);

        for (int i = 0; i < inventory.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack itemStack = inventory.getInventory().getItem(i);
            if (!itemStack.isEmpty()) {
                invOutput.add(new ItemStackWithSlot(i, itemStack));
            }
        }

        //The output automatically updates the compound tag, no need to manually add anything
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

        //in order to load the inventory, we need a ServerPlayer, server players require this
        //data. Defaults are fine, this is only used to load the inventory
        MinecraftServer server = MinecraftServer.getServer();
        ServerLevel serverLevel = server.getLevel(Level.OVERWORLD);
        if(serverLevel == null) return null;

        GameProfile gameProfile = new GameProfile(id, name);
        ClientInformation clientOptions= ClientInformation.createDefault();
        ServerPlayer serverPlayer = new ServerPlayer(server, serverLevel, gameProfile, clientOptions);

        ValueInput loadedData = server.getPlayerList().playerIo.load(serverPlayer, ProblemReporter.DISCARDING).orElse(null);
        if(loadedData == null) return null;

        //create Minecraft Inventory
        Inventory inventory = serverPlayer.getInventory();

        inventory.load(loadedData.listOrEmpty("Inventory", ItemStackWithSlot.CODEC));


        //return the BukkitPlayerInventory (same one you get from player#getInventory())
        return new CraftInventoryPlayer(inventory);

    }
}
