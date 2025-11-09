package me.mykindos.betterpvp.core.block.impl.workbench;

import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.button.BackTabButton;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import net.kyori.adventure.text.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GuiQuickCraftViewer extends AbstractPagedGui<QuickCraftingButton> {

    private static final URL url;

    static {
        try {
            url = URI.create("https://wiki.betterpvp.net/").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private final GuiWorkbench parent;

    GuiQuickCraftViewer(GuiWorkbench parentWorkbench) {
        super(9, 6, false, new Structure(
                "0000000BI",
                        "0XXXXXXX0",
                        "0XXXXXXX0",
                        "0XXXXXXX0",
                        "000000000",
                        "000<0>000")
                .addIngredient('X', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', PageBackwardButton.defaultTexture())
                .addIngredient('>', PageForwardButton.defaultTexture())
                .addIngredient('B', new BackTabButton(parentWorkbench::setCraftingTab))
                .addIngredient('I', InfoTabButton.builder()
                        // todo: wiki entry
                        .wikiEntry("Test", url)
                        .description(Component.text("Click on an item to quickly place its ingredients into the workbench."))
                        .build())
                );
        this.parent = parentWorkbench;
        refresh();
    }

    public void refresh() {
        List<QuickCraftingButton> buttons = new ArrayList<>();
        for (int i = 0; i < parent.quickCrafts.size(); i++) {
            buttons.add(new QuickCraftingButton(i, parent));
        }
        setContent(buttons);
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (Item item : content) {
            page.add(new SlotElement.ItemSlotElement(item));

            if (page.size() >= contentSize) {
                pages.add(page);
                page = new ArrayList<>(contentSize);
            }
        }

        if (!page.isEmpty()) {
            pages.add(page);
        }

        this.pages = pages;
        update();
    }
}
