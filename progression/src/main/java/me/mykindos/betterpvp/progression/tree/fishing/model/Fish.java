package me.mykindos.betterpvp.progression.tree.fishing.model;

import lombok.Value;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Value
public class Fish {

    public static final Material[] fishBuckets = new Material[] {
            Material.COD_BUCKET,
            Material.SALMON_BUCKET,
            Material.TROPICAL_FISH_BUCKET,
            Material.PUFFERFISH_BUCKET,
            Material.AXOLOTL_BUCKET
    };

    private static final Random RANDOM = new Random();

    FishType type;
    int weight;

    public ItemStack generateItem() {
        // get a random fish bucket
        Material randomType = fishBuckets[RANDOM.nextInt(fishBuckets.length)];
        ItemStack item = new ItemStack(randomType);

        // Display
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm");
        String date = simpleDateFormat.format(new Date());
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(this.type.getName(), NamedTextColor.GREEN));
        meta.lore(List.of(
                Component.empty(),
                UtilMessage.deserialize("<dark_gray>                    "),
                Component.empty(),
                UtilMessage.deserialize("<gray>Weight: <yellow>" + weight + "kg"),
                UtilMessage.deserialize("<gray>Caught: <yellow>" + date),
                Component.empty(),
                UtilMessage.deserialize("<dark_gray>                    "),
                Component.empty()
        ));

        // PDC tags
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ProgressionNamespacedKeys.FISHING_FISH_TYPE, PersistentDataType.STRING, type.getName());
        pdc.set(ProgressionNamespacedKeys.FISHING_FISH_WEIGHT, PersistentDataType.INTEGER, weight);

        item.setItemMeta(meta);
        return item;
    }

}
