package me.mykindos.betterpvp.core.item.pagination;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.item.renderer.LorePages;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Tracks which lore page each viewer is currently looking at, per item.
 * <p>
 * State is keyed by {@code (viewer, itemIdentity)} and held in a bounded, expiring cache so it
 * can never grow without limit no matter how many distinct items a player inspects - entries
 * fall out after a short idle window and the total is capped. Nothing here needs explicit
 * cleanup on logout.
 */
@Singleton
public class LorePageService {

    private final Cache<PageKey, Integer> pages = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(10_000)
            .build();

    /**
     * Resolve the page a viewer should currently see for an item. Defaults to the item's most
     * relevant page, and snaps any stored page back onto the currently visible set.
     */
    public int resolve(UUID viewer, ItemInstance item) {
        final Integer stored = pages.getIfPresent(new PageKey(viewer, itemIdentity(item)));
        if (stored == null) {
            return LorePages.mostRelevant(item);
        }
        return LorePages.clamp(item, stored);
    }

    /**
     * Advance a viewer to the next visible page of an item, wrapping back to the first.
     * No-op when the item has fewer than two visible pages.
     */
    public void advance(UUID viewer, ItemInstance item) {
        final List<Integer> visible = LorePages.visiblePages(item);
        if (visible.size() <= 1) {
            return;
        }

        final int current = resolve(viewer, item);
        final int currentIndex = Math.max(0, visible.indexOf(current));
        final int next = visible.get((currentIndex + 1) % visible.size());
        pages.put(new PageKey(viewer, itemIdentity(item)), next);
    }

    /**
     * A stable identity for an item: its unique id when it is a non-stackable {@link UUIDProperty}
     * item, otherwise its content (equal content shares page state, which is acceptable).
     */
    Object itemIdentity(ItemInstance item) {
        return item.getComponent(UUIDProperty.class)
                .map(UUIDProperty::getUniqueId)
                .map(Object.class::cast)
                .orElseGet(item::getItemStack);
    }

    @Value
    private static class PageKey {
        UUID viewer;
        Object itemIdentity;
    }

}
