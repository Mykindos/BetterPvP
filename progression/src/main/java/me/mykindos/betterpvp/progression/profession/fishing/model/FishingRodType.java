package me.mykindos.betterpvp.progression.profession.fishing.model;

import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
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
 */
public interface FishingRodType extends ConfigAccessor {

    int getId();

    @NotNull String getName();

    boolean canReel(@NotNull LootBundle bundle);

    Component[] getDescription();

    default ItemStack getPlayerFriendlyItem() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);

        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(getName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        final ArrayList<Component> lore = new ArrayList<>();
        lore.addAll(List.of(Component.empty(), UtilMessage.DIVIDER, Component.empty()));
        lore.addAll(List.of(getDescription()));
        lore.addAll(List.of(Component.empty(), UtilMessage.DIVIDER, Component.empty()));
        meta.lore(lore);

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ProgressionNamespacedKeys.FISHING_ROD_TYPE, PersistentDataType.INTEGER, getId());
        pdc.set(CoreNamespaceKeys.IMMUTABLE_KEY, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }
}
