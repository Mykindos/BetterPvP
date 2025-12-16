package me.mykindos.betterpvp.core.client.stats.display;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.display.filter.PeriodFilterButton;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterButton;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class AbstractStatPagedMenu extends AbstractPagedGui<Item> implements IAbstractStatMenu {
    @NotNull
    private final Client client;
    @Nullable
    private final Windowed previous;
    private final StatPeriodManager statPeriodManager;

    private final StringFilterButton<IAbstractStatMenu> periodFilterButton;

    private String periodKey;
    @SuppressWarnings("unchecked")
    protected AbstractStatPagedMenu(@NotNull Client client, @Nullable Windowed previous, String periodKey, StatPeriodManager statPeriodManager) {
        super(9, 6, false, new Structure(
                            "# # # # # # # # P",
                            "# x x x x x x x #",
                            "# x x x x x x x #",
                            "# x x x x x x x #",
                            "# x x x x x x x #",
                            "# # # < - > # # #")
                    .addIngredient('x',Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                    .addIngredient('#',Menu.BACKGROUND_ITEM)
                    .addIngredient('<', new PageBackwardButton())
                    .addIngredient('-', new StatBackButton(previous))
                    .addIngredient('>', new PageForwardButton())
                    .addIngredient('P', new PeriodFilterButton(periodKey, statPeriodManager))
        );
        if (!(getItem(8, 0) instanceof StringFilterButton<?> periodButton)) throw new IllegalStateException("Item in this slot must be a StringFilterButton");
        this.periodFilterButton = (StringFilterButton<IAbstractStatMenu>) periodButton;

        //todo set back button runnable to update current period

        this.client = client;
        this.previous = previous;
        this.statPeriodManager = statPeriodManager;

        this.periodKey = periodKey;
    }

    /**
     * @return The title of this menu.
     */
    @Override
    public @NotNull Component getTitle() {
        return Component.text(client.getName() + "'s Stats");
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
