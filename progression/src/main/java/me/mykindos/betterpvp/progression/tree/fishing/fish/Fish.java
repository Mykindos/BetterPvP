package me.mykindos.betterpvp.progression.tree.fishing.fish;

import lombok.Value;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

@Value
public class Fish implements FishingLoot {

    public static final Material[] fishBuckets = new Material[]{
            Material.COD_BUCKET,
            Material.SALMON_BUCKET,
            Material.TROPICAL_FISH_BUCKET,
            Material.PUFFERFISH_BUCKET,
            Material.AXOLOTL_BUCKET
    };

    private static final Random RANDOM = new Random();

    FishType type;
    int weight;

    public static boolean isFishItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }

        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(ProgressionNamespacedKeys.FISHING_FISH_TYPE, PersistentDataType.STRING);
    }

    @Override
    public void processCatch(PlayerCaughtFishEvent event) {
        SimpleFishType fishType = (SimpleFishType) type;
        final Item entity = (Item) event.getCaught();
        if (entity != null) {
            int currentWeight = weight;

            while (currentWeight > 0) {
                int dropWeight = Math.min(currentWeight, 64);
                currentWeight -= dropWeight;
                ItemStack drop = new ItemStack(fishType.getMaterial(), dropWeight);
                drop.editMeta(meta -> meta.setCustomModelData((fishType.getModelData())));
                Item item = entity.getWorld().dropItem(entity.getLocation(), drop);

                // For some reason the entity doesnt have the correct velocity at the time of execution, wait 1 tick.
                UtilServer.runTaskLater(JavaPlugin.getPlugin(Progression.class), () -> item.setVelocity(entity.getVelocity()), 1);

            }

            entity.setItemStack(new ItemStack(Material.AIR));
            UtilMessage.message(event.getPlayer(), "Fishing", "You caught a <alt>%s</alt> (<alt2>%slb</alt2>)!",
                    type.getName(), UtilFormat.formatNumber(weight));
        }
    }
}
