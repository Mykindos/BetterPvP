package me.mykindos.betterpvp.progression.tree.fishing.model;

import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.List;

public interface BaitType extends ConfigAccessor {

    /**
     * Get the name of the bait
     * @return The name of the bait
     */
    @NotNull String getName();

    /**
     * Get the expiration for this bait
     * @return The expiration, in seconds.
     */
    double getExpiration();

    /**
     * Get the radius of the bait
     * @return The radius of the bait
     */
    @Range(from = 0, to = Integer.MAX_VALUE) double getRadius();

    /**
     * Get a bait instance of this type
     * @return The bait instance
     */
    @NotNull Bait generateBait();

    /**
     * Get the itemstack for this bait to be used in heads
     * @return The itemstack
     */
    ItemStack getRawItem();

    /**
     * Get the description for this bait
     * @return The description
     */
    Component[] getDescription();

    /**
     * Get the itemstack for this bait, but with a player-friendly display
     * @return The itemstack
     */
    default ItemStack getPlayerFriendlyItem() {
        // get a random fish bucket
        ItemStack item = getRawItem();

        // Display
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(getName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        final ArrayList<Component> lore = new ArrayList<>();
        lore.addAll(List.of(Component.empty(), UtilMessage.DIVIDER, Component.empty()));
        lore.addAll(List.of(getDescription()));
        lore.addAll(List.of(Component.empty(), UtilMessage.DIVIDER, Component.empty()));
        meta.lore(lore);

        // PDC tags
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ProgressionNamespacedKeys.FISHING_BAIT_TYPE, PersistentDataType.STRING, getName());
        pdc.set(CoreNamespaceKeys.IMMUTABLE_KEY, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

}
