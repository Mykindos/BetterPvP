package me.mykindos.betterpvp.core.menu.impl;

import me.mykindos.betterpvp.core.inventory.gui.AbstractScrollGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.button.ScrollLeftButton;
import me.mykindos.betterpvp.core.menu.button.ScrollRightButton;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A band of content with an arrow pinned to each end.
 * <p>
 * Content reads like text: left to right along a row, wrapping to the row below, with the arrows
 * scrolling a row at a time. Intended to be nested inside a larger menu with
 * {@link me.mykindos.betterpvp.core.inventory.gui.Gui#fillRectangle}, which is what lets one screen
 * hold several independently scrolling bands.
 */
public class HorizontalScrollGui extends AbstractScrollGui<Item> {

    public HorizontalScrollGui(int width, int height) {
        super(width, height, false, structure(width, height));

        setLineLength(width - 2);
        setLineAmount(height);
    }

    private static Structure structure(int width, int height) {
        final StringBuilder row = new StringBuilder(width);
        row.append('<');
        row.repeat(".", width - 2);
        row.append('>');

        final String[] rows = new String[height];
        Arrays.fill(rows, row.toString());

        return new Structure(rows)
                .addIngredient('<', ScrollLeftButton.defaultTexture())
                .addIngredient('>', ScrollRightButton.defaultTexture())
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL);
    }

    @Override
    public void bake() {
        final List<SlotElement> elements = new ArrayList<>(content.size());
        for (Item item : content) {
            elements.add(new SlotElement.ItemSlotElement(item));
        }

        this.elements = elements;
        update();
    }

    /**
     * Replaces the band's contents, keeping the current scroll position where it is still valid.
     */
    public void setItems(@NotNull List<Item> items) {
        setContent(items);
    }
}
