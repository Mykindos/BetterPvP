package me.mykindos.betterpvp.progression.tree.fishing.fish;

import lombok.Value;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Value
public class Fish implements FishingLoot {

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

    public ItemStack getFishBucket() {
        // get a random fish bucket
        Material randomType = fishBuckets[RANDOM.nextInt(fishBuckets.length)];
        ItemStack item = new ItemStack(randomType);

        // Display
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm");
        String date = simpleDateFormat.format(new Date());
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(this.type.getName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                UtilMessage.DIVIDER,
                Component.empty(),
                UtilMessage.deserialize("<gray>Weight: <yellow>" + weight + "lb").decoration(TextDecoration.ITALIC, false),
                UtilMessage.deserialize("<gray>Caught: <yellow>" + date).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                UtilMessage.DIVIDER,
                Component.empty()
        ));

        // PDC tags
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ProgressionNamespacedKeys.FISHING_FISH_TYPE, PersistentDataType.STRING, type.getName());
        pdc.set(ProgressionNamespacedKeys.FISHING_FISH_WEIGHT, PersistentDataType.INTEGER, weight);
        pdc.set(CoreNamespaceKeys.IMMUTABLE_KEY, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void processCatch(PlayerCaughtFishEvent event) {
        ItemStack item = getFishBucket();
        final Item entity = (Item) event.getCaught();
        Objects.requireNonNull(entity).setItemStack(item);
        UtilMessage.message(event.getPlayer(), "Fishing", "You caught a <alt>%s</alt> (<alt2>%slb</alt2>)!", type.getName(), weight);
    }
}
