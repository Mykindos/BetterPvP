package me.mykindos.betterpvp.core.inventory.item.impl;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.renderer.LorePages;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A display-only menu {@link me.mykindos.betterpvp.core.inventory.item.Item Item} that flips an
 * {@link ItemInstance}'s lore pages when the viewer presses the offhand/swap key (F) while hovering
 * it - the menu counterpart to {@link me.mykindos.betterpvp.core.item.pagination.LorePaginationListener},
 * which only handles the player's own inventory.
 * <p>
 * This is the manual, viewer-driven alternative to {@link RotatingLoreItem}: there is no timer and no
 * {@link me.mykindos.betterpvp.core.item.pagination.LoreRotationClock} registration, so a tile only
 * changes when the viewer asks it to. Use it when the page should stay put until acted upon (e.g. so
 * a player can read a page without it rotating away); use {@link RotatingLoreItem} when the page
 * should cycle on its own.
 * <p>
 * Performance: each visible page is rendered <b>once</b> and cached, so a flip only clones a
 * pre-rendered {@link ItemStack} and re-applies the decorator - the lore renderer never runs on the
 * click path. Single-page items simply ignore the key.
 */
public class PaginatedLoreItem extends AbstractItem {

    private final ItemInstance instance;
    private final UnaryOperator<ItemStack> decorator;
    private final List<Integer> pages;
    private final Map<Integer, ItemStack> cache = new HashMap<>();
    private int index;

    /**
     * @param instance  the item to display
     * @param decorator optional post-processing applied to each rendered (cloned) page stack
     *                  - e.g. appending price/expiry lore - or {@code null} for none
     */
    public PaginatedLoreItem(ItemInstance instance, UnaryOperator<ItemStack> decorator) {
        this.instance = instance;
        this.decorator = decorator;
        this.pages = LorePages.visiblePages(instance);
    }

    @Override
    public ItemProvider getItemProvider() {
        final int page = pages.isEmpty() ? LorePages.mostRelevant(instance) : pages.get(index % pages.size());
        final ItemStack base = cache.computeIfAbsent(page, p -> instance.getView().get(null, p));
        ItemStack stack = base.clone();
        if (decorator != null) {
            stack = decorator.apply(stack);
        }
        return ItemView.of(stack);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType != ClickType.SWAP_OFFHAND || pages.size() <= 1) {
            return; // nothing to page through - leave the tile as-is
        }
        index = (index + 1) % pages.size();
        notifyWindows();
        new SoundEffect(Sound.BLOCK_METAL_PRESSURE_PLATE_CLICK_ON, 2f, 1f).play(player);
    }

}
