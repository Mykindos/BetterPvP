package me.mykindos.betterpvp.core.item.component.impl.socketables;

import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.socketables.runes.RuneItem;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static me.mykindos.betterpvp.core.utilities.UtilMessage.miniMessage;

/**
 * Represents a rune that can be applied to items.
 * Runes are special enhancements that can modify the properties of items.
 *
 * @see RuneItem
 */
public interface Socketable {

    static boolean isDamageable(@NotNull Item item) {
        final ItemStack itemStack;
        if (item instanceof ItemInstance itemInstance) {
            itemStack = itemInstance.createItemStack();
        } else if (item instanceof BaseItem baseItem) {
            itemStack = baseItem.getModel();
        } else {
            throw new IllegalArgumentException("Item must be an ItemInstance or BaseItem");
        }
        return itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof Damageable;
    }

    /**
     * Gets the namespaced key that is used when this rune is applied to an item.
     *
     * @return The namespaced key
     */
    @NotNull NamespacedKey getKey();

    /**
     * Gets the name of this rune.
     *
     * <p>This is a stable identity string used for modifiers, logging and lookups; it is NOT localized.
     * For the localized display name shown in item name/lore, use {@link #getDisplayName()}.</p>
     *
     * @return The name of the rune
     */
    @NotNull String getName();

    /**
     * Gets the localized display name component of this rune, rendered per viewer at the packet boundary.
     *
     * <p>Defaults to a plain text component wrapping {@link #getName()}. Translatable runes override this to
     * return a {@code Translations.component("core.item.rune.<name>.name")} translatable component.</p>
     *
     * @return The display name component of the rune
     */
    default @NotNull Component getDisplayName() {
        return Component.text(getName());
    }

    /**
     * Gets the description of this rune.
     *
     * @return The description of the rune
     */
    @NotNull String getDescription();

    /**
     * Gets the localized, line-wrapped description of this rune, rendered per viewer at the packet boundary.
     *
     * <p>Defaults to deserializing {@link #getDescription()} (with its inline MiniMessage tags) under a gray
     * base colour. Translatable runes override this to return tag-free, translatable lines via
     * {@code Translations.componentLines("core.item.rune.<name>.lore", styledArgs...)}.</p>
     *
     * @return The description lines of the rune
     */
    default @NotNull List<Component> getDescriptionLines() {
        return ComponentWrapper.wrapLine(miniMessage.deserialize("<gray>" + getDescription()));
    }

    /**
     * Gets the groups that this rune belongs to.
     * @return An array of rune groups that this rune belongs to
     */
    @NotNull Collection<@NotNull SocketableGroup> getGroups();

    /**
     * Gets the stats provided by this socketable.
     *
     * @return A collection of stats provided by this socketable
     */
    default @NotNull Collection<ItemStat<?>> getStats() {
        return Collections.emptyList();
    }

    /**
     * Checks if this rune can be applied to the specified item.
     *
     * @param item The item instance to check
     * @return true if the rune can be applied
     */
    default boolean canApply(@NotNull Item item) {
        return getGroups().stream().anyMatch(group -> group.canApply(item));
    }

}
