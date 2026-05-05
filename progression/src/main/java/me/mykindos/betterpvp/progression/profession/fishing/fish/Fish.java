package me.mykindos.betterpvp.progression.profession.fishing.fish;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Plain data object representing a caught fish.
 * Weight may be mutated by skills (ThickerLines, CatchWeightAttribute) before the drop is awarded.
 */
@Data
@AllArgsConstructor
public class Fish {

    private UUID uuid;
    /** Display name / type name for leaderboard persistence. */
    private String typeName;
    private int weight;

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

    /** Convenience accessor matching the old {@code fish.getType().getName()} call sites. */
    public String getDisplayName() {
        return typeName;
    }

    @Override
    public String toString() {
        return UtilFormat.formatNumber(weight) + "lb " + typeName;
    }
}
