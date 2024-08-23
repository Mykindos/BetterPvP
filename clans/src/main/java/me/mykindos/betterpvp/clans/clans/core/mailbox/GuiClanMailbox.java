package me.mykindos.betterpvp.clans.clans.core.mailbox;

import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GuiClanMailbox extends AbstractPagedGui<Item> implements Windowed {

    private final String title;

    public GuiClanMailbox(ClanMailbox clanMailbox, @Nullable Windowed previous) {
        super(9, 6, false, new Structure(
                "# # # # # # # # #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PreviousButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new ForwardButton()));
        this.title = "Mailbox";

        List<Item> mailboxItems = clanMailbox.getContents().stream().map(item -> new ClanMailboxItem(clanMailbox, item))
                .map(Item.class::cast)
                .toList();
        setContent(mailboxItems);
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
