package me.mykindos.betterpvp.core.droptables;

import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * A specialized ItemStack for use with DropTable that encapsulates min and max amount properties.
 * This eliminates the need to store these values in the persistent data container.
 */
public class DropTableItemStack extends ItemStack {

    @Getter
    private final int minAmount;

    @Getter
    private final int maxAmount;

    /**
     * Creates a new DropTableItemStack with the given material, custom model data, and min/max amounts.
     *
     * @param material The material of the item
     * @param customModelData The custom model data of the item (0 for none)
     * @param minAmount The minimum amount that can be rolled
     * @param maxAmount The maximum amount that can be rolled
     */
    public DropTableItemStack(Material material, int customModelData, int minAmount, int maxAmount) {
        super(material, 1);
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;

        if (customModelData > 0) {
            ItemMeta meta = getItemMeta();
            meta.setCustomModelData(customModelData);
            setItemMeta(meta);
        }
    }

    /**
     * Creates a new DropTableItemStack from an existing ItemStack with the given min/max amounts.
     *
     * @param itemStack The ItemStack to copy
     * @param minAmount The minimum amount that can be rolled
     * @param maxAmount The maximum amount that can be rolled
     */
    public DropTableItemStack(ItemStack itemStack, int minAmount, int maxAmount) {
        super(itemStack);
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    /**
     * Creates a copy of this DropTableItemStack with a random amount between min and max.
     *
     * @return A new ItemStack with a random amount
     */
    public ItemStack create() {
        ItemStack result = clone();
        int amount = UtilMath.randomInt(minAmount, maxAmount);
        result.setAmount(amount);
        return result;
    }

    @Override
    public @NotNull DropTableItemStack clone() {
        return new DropTableItemStack(super.clone(), minAmount, maxAmount);
    }
}