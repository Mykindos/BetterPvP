package me.mykindos.betterpvp.core.client.stats.display;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.filter.RealmFilterButton;
import me.mykindos.betterpvp.core.client.stats.display.filter.SeasonFilterButton;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.server.Period;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public abstract class AbstractStatPagedMenu extends AbstractPagedGui<Item> implements IAbstractStatMenu {
    @NotNull
    private final Client client;
    @Nullable
    private final Windowed previous;
    private final RealmManager realmManager;

    private final SeasonFilterButton seasonFilterButton;
    private final RealmFilterButton realmFilterButton;

    private StatFilterType type;
    private Period period;


    @SuppressWarnings("unchecked")
    protected AbstractStatPagedMenu(@NotNull Client client, @Nullable Windowed previous, StatFilterType type, @Nullable Period period, RealmManager realmManager) {
        super(9, 6, false, new Structure(
                            "# # # # # # # S R",
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
                    //todo fix these monstrosities
                    .addIngredient('S', new SeasonFilterButton(
                            IAbstractStatMenu.getSeasonContext(type, period),
                            IAbstractStatMenu.getSeasonContexts(realmManager)))
                    .addIngredient('R', new RealmFilterButton(
                            IAbstractStatMenu.getRealmContext(type, period),
                            IAbstractStatMenu.getRealmContexts(IAbstractStatMenu.getSeasonContext(type, period) == null ? null : Objects.requireNonNull(IAbstractStatMenu.getSeasonContext(type, period)).getSeason(), realmManager)))
        );
        if (!(getItem(7, 0) instanceof SeasonFilterButton seasonButton)) throw new IllegalStateException("Item in this slot must be a SeasonFilterButton");
        this.seasonFilterButton = seasonButton;
        if (!(getItem(8, 0) instanceof RealmFilterButton realmButton)) throw new IllegalStateException("Item in this slot must be a RealmFilterButton");
        this.realmFilterButton = realmButton;

        this.client = client;
        this.previous = previous;
        this.realmManager = realmManager;

        this.type = type;
        this.period = period;
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
