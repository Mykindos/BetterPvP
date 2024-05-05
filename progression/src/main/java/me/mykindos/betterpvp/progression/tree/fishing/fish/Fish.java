package me.mykindos.betterpvp.progression.tree.fishing.fish;

import lombok.Value;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
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
        ItemStack item = new ItemStack(fishType.getMaterial(), weight);
        item.editMeta(meta -> meta.setCustomModelData((fishType.getModelData())));
        final Item entity = (Item) event.getCaught();
        Objects.requireNonNull(entity).setItemStack(item);
        UtilMessage.message(event.getPlayer(), "Fishing", "You caught a <alt>%s</alt> (<alt2>%slb</alt2>)!", type.getName(), weight);
    }
}
