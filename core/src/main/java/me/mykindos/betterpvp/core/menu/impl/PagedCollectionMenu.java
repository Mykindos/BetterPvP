package me.mykindos.betterpvp.core.menu.impl;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.PagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

/**
 * Represents a paged menu that retrieves its page content when the page is updated, not at create time
 * setContent is not supported
 * @param <C> The item type of the content
 */
@CustomLog
public abstract class PagedCollectionMenu<C extends Item> extends AbstractGui implements PagedGui<C> {
    private final Structure structure;
    private int currentPage;
    private List<BiConsumer<Integer, Integer>> pageChangeHandlers;
    private final ReentrantLock updateLock = new ReentrantLock();


    protected PagedCollectionMenu(Structure structure) {
        super(structure.getWidth(), structure.getHeight());
        this.structure = structure;
        this.currentPage = 0;
        applyStructure(structure);
    }


    /**
     * Retrieves the content for the specified page number
     * @param page the page number (0 is first)
     * @param amount (the number of items per page)
     * @return the list of items for the page
     */
    protected abstract CompletableFuture<List<C>> getPage(int page, int amount);

    protected void update() {
        correctCurrentPage();
        updateControlItems();
        updatePageContent();
    }

    private void correctCurrentPage() {
        int correctedPage = correctPage(currentPage);
        if (correctedPage != currentPage)
            setPage(correctedPage);
    }

    private void updatePageContent() {
        if (!updateLock.tryLock()) {
            return;
        }
        try {
            int contentSize = getContentListSlots().length;
            getPage(currentPage, contentSize).handle((items, throwable) -> {
                if (throwable != null) {
                    log.error("Error updating page {} in PagedCollectionMenu", currentPage, throwable).submit();
                    return false;
                }
                List<SlotElement.ItemSlotElement> slotElements = items.stream()
                        .map(SlotElement.ItemSlotElement::new)
                        .toList();
                for (int i = 0; i < contentSize; i++) {
                    if (slotElements.size() > i) setSlotElement(getContentListSlots()[i], slotElements.get(i));
                    else remove(getContentListSlots()[i]);
                }
                return true;
            });
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * Gets the amount of pages this {@link PagedGui} has.
     *
     * @return The amount of pages this {@link PagedGui} has.
     */
    @Override
    public int getPageAmount() {
        return Integer.MAX_VALUE;
    }

    /**
     * Gets the current page of this {@link PagedGui} as an index.
     *
     * @return Gets the current page of this {@link PagedGui} as an index.
     */
    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Sets the current page of this {@link PagedGui}.
     *
     * @param page The page to set.
     */
    @Override
    public void setPage(int page) {
        int previousPage = currentPage;
        int newPage = correctPage(page);

        if (previousPage == newPage)
            return;

        currentPage = newPage;
        update();

        if (getPageChangeHandlers() != null) {
            getPageChangeHandlers().forEach(handler -> handler.accept(previousPage, newPage));
        }
    }

    private int correctPage(int page) {
        // page 0 always exist

        return Math.max(page, 0);
        // 0 <= page < pageAmount
    }

    /**
     * Checks if there is a next poge
     * Updates the maxPages if there is a next page
     * @return whether there is a next page or not
     */
    @Override
    public boolean hasNextPage() {
        return true;
    }

    /**
     * Checks if there is a previous page.
     *
     * @return Whether there is a previous page.
     */
    @Override
    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    /**
     * Gets if there are infinite pages in this {@link PagedGui}.
     *
     * @return Whether there are infinite pages in this {@link PagedGui}.
     */
    @Override
    public boolean hasInfinitePages() {
        return true;
    }

    /**
     * Displays the next page if there is one.
     */
    @Override
    public void goForward() {
        if (hasNextPage())
            setPage(currentPage + 1);
    }

    /**
     * Displays the previous page if there is one.
     */
    @Override
    public void goBack() {
        if (hasPreviousPage())
            setPage(currentPage - 1);
    }

    /**
     * Gets the slot indices that are used to display content in this {@link PagedGui}.
     *
     * @return The slot indices that are used to display content in this {@link PagedGui}.
     */
    @Override
    public int[] getContentListSlots() {
        return structure.getIngredientList().findContentListSlots();
    }

    /**
     * Unused
     */
    @Override
    public void setContent(@Nullable List<@NotNull C> content) {
        throw new UnsupportedOperationException("setContent is not usable for this");
    }

    @Override
    public void bake() {
        throw new UnsupportedOperationException("bake is not usable for this");
    }

    @Override
    public void addPageChangeHandler(@NotNull BiConsumer<Integer, Integer> pageChangeHandler) {
        if (getPageChangeHandlers() == null) {
            setPageChangeHandlers(new ArrayList<>());
        }

        getPageChangeHandlers().add(pageChangeHandler);
    }

    @Override
    public void removePageChangeHandler(@NotNull BiConsumer<Integer, Integer> pageChangeHandler) {
        if (getPageChangeHandlers() != null) {
            getPageChangeHandlers().remove(pageChangeHandler);
        }
    }

    @Override
    public void setPageChangeHandlers(@Nullable List<@NotNull BiConsumer<Integer, Integer>> handlers) {
        this.pageChangeHandlers = handlers;
    }

    @Nullable
    public List<BiConsumer<Integer, Integer>> getPageChangeHandlers() {
        return pageChangeHandlers;
    }
}
