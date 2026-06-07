package me.mykindos.betterpvp.core.item.renderer;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.LoreComponent;

import java.util.List;

/**
 * Pure helpers for resolving the set of lore pages an {@link ItemInstance} exposes.
 * <p>
 * A page is an open-ended {@code int} declared by each {@link LoreComponent#getLorePage()}.
 * Only pages with at least one component producing non-empty lines are considered "visible";
 * everything in this class operates over that visible set.
 */
@UtilityClass
public class LorePages {

    /**
     * The ordered, distinct set of pages this item actually renders content on.
     *
     * @param item the item to inspect
     * @return sorted distinct visible page indices (may be empty)
     */
    public List<Integer> visiblePages(ItemInstance item) {
        return item.getComponents().stream()
                .filter(LoreComponent.class::isInstance)
                .map(LoreComponent.class::cast)
                .filter(component -> !component.getLines(item).isEmpty())
                .map(LoreComponent::getLorePage)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * The page to show when no specific page is requested - the lowest visible page.
     *
     * @param item the item to inspect
     * @return the most relevant page, or {@code 0} if the item has no visible pages
     */
    public int mostRelevant(ItemInstance item) {
        final List<Integer> visible = visiblePages(item);
        return visible.isEmpty() ? 0 : visible.getFirst();
    }

    /**
     * Snaps an arbitrary page index onto the item's visible pages, picking the nearest
     * visible page (ties resolve to the lower page). Keeps stored cursors valid when the
     * visible set changes underneath them.
     *
     * @param item the item to inspect
     * @param page the requested page index
     * @return the nearest visible page, or {@code 0} if the item has no visible pages
     */
    public int clamp(ItemInstance item, int page) {
        final List<Integer> visible = visiblePages(item);
        if (visible.isEmpty()) {
            return 0;
        }
        if (visible.contains(page)) {
            return page;
        }

        int best = visible.getFirst();
        int bestDistance = Math.abs(best - page);
        for (int candidate : visible) {
            final int distance = Math.abs(candidate - page);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

}
