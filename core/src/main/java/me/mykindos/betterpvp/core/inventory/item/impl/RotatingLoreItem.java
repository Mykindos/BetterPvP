package me.mykindos.betterpvp.core.inventory.item.impl;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.renderer.LorePages;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A display-only {@link me.mykindos.betterpvp.core.inventory.item.Item Item} that cycles an
 * {@link ItemInstance}'s lore pages over time, for menus where the viewer can't press the
 * offhand key (they aren't holding the item).
 * <p>
 * Performance: each visible page is rendered <b>once</b> and cached, so a rotation tick only swaps
 * a pre-rendered {@link ItemStack} - the lore renderer never runs on tick. Single-page items never
 * start a timer at all. Intended for low-cardinality displays; high-cardinality menus
 * (e.g. the item viewer) should drive rotation through {@code LoreRotationClock} instead of one
 * timer per tile.
 */
public class RotatingLoreItem extends AutoUpdateItem {

    private final int pageCount;

    /**
     * @param instance    the item to display
     * @param periodTicks how often (in ticks) to advance the page
     * @param decorator   optional post-processing applied to each rendered (cloned) page stack
     *                    - e.g. appending price/expiry lore - or {@code null} for none
     */
    public RotatingLoreItem(ItemInstance instance, int periodTicks, UnaryOperator<ItemStack> decorator) {
        super(periodTicks, supplier(instance, decorator));
        this.pageCount = LorePages.visiblePages(instance).size();
    }

    private static Supplier<? extends ItemProvider> supplier(ItemInstance instance, UnaryOperator<ItemStack> decorator) {
        final List<Integer> pages = LorePages.visiblePages(instance);
        final Map<Integer, ItemStack> cache = new HashMap<>();
        final int[] index = {0};
        return () -> {
            final int page = pages.isEmpty() ? LorePages.mostRelevant(instance) : pages.get(index[0] % pages.size());
            if (!pages.isEmpty()) {
                index[0] = (index[0] + 1) % pages.size();
            }
            final ItemStack base = cache.computeIfAbsent(page, p -> instance.getView().get(null, p));
            ItemStack stack = base.clone();
            if (decorator != null) {
                stack = decorator.apply(stack);
            }
            return ItemView.of(stack);
        };
    }

    @Override
    public void start() {
        // Nothing to rotate through - never spin up a timer for a single-page item.
        if (pageCount <= 1) {
            return;
        }
        super.start();
    }

}
