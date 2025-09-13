package me.mykindos.betterpvp.core.logging.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.PreviousableButton;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LogRepositoryMenu extends AbstractPagedGui<Item> implements Windowed {
    private final String title;
    public LogRepositoryMenu(@NotNull String title, List<? extends PreviousableButton> pool, Windowed previous) {
        super(9, 3, false, new Structure(
                "# # # # # # # # #",
                "# x x x x x x x #",
                "# # # < - > # # R")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PageBackwardButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new PageForwardButton()));
        this.title = title;
        pool.forEach(previousableButton -> previousableButton.setPrevious(this));
        setContent(pool.stream().map(Item.class::cast).toList());
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text(title);
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
