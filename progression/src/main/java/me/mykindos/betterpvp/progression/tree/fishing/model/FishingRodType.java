package me.mykindos.betterpvp.progression.tree.fishing.model;

import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.tree.fishing.fish.FishType;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a type of fishing rod.
 * Different types of fishing rods are <i>required</i> to catch some {@link FishType}s.
 */
public interface FishingRodType extends ConfigAccessor {

    /**
     * A unique ID for the fishing rod type, DO NOT CHANGE
     * @return The ID of the fishing rod type
     */
    int getId();

    /**
     * @return The name of the fishing rod type
     */
    @NotNull String getName();

    /**
     * Determines if the fishing rod can reel in the loot
     * @param loot The loot
     * @return {@code true} if the fishing rod can reel in the loot, {@code false} otherwise
     */
    boolean canReel(@NotNull FishingLoot loot);

    /**
     * Get the description for this rod
     * @return The description
     */
    Component[] getDescription();

    /**
     * Get the itemstack for this rod, but with a player-friendly display
     * @return The itemstack
     */
    default ItemStack getPlayerFriendlyItem() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);

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
        pdc.set(ProgressionNamespacedKeys.FISHING_ROD_TYPE, PersistentDataType.INTEGER, getId());
        pdc.set(CoreNamespaceKeys.IMMUTABLE_KEY, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }


}
