package me.mykindos.betterpvp.core.client.stats.display;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.formatter.category.IStatCategory;
import me.mykindos.betterpvp.core.client.stats.formatter.manager.StatFormatters;
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
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@CustomLog
public class StatMenu extends AbstractPagedGui<Item> implements Windowed {
    private IStatCategory statCategory;
    private final Client client;
    private String period;

    public StatMenu(Client client) {
        this(client, null, "", null);
    }

    public StatMenu(Client client, @Nullable IStatCategory iStatCategory, String period, @Nullable Windowed previous) {
        super(9, 5, false, new Structure("# # # # # # # # #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PreviousButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new ForwardButton())
        );
        //todo add period button
        this.client = client;
        this.statCategory = iStatCategory;
        this.period = period;
        setContent(getItems());
    }

    private List<Item> getItems() {
        final StatContainer container = client.getStatContainer();
        Collection<IStatCategory> childCategories;
        //reset the achievement category if it is no longer valid
        //get the category buttons
        if (statCategory != null) {
            childCategories = statCategory.getChildren();
        } else {
            childCategories = StatFormatters.getRootCategories();
        }


        log.info("Num Child Categories {}", childCategories.size()).submit();

        //add the categories
        final List<Item> items = new ArrayList<>(childCategories.stream()
                .map(category->
                        //todo action to category button
                    category.getDescription().toBuilder()
                            .clickFunction((click) -> {
                                new StatMenu(client, category, period, this).show(click.getPlayer());
                            })
                            .build()
                )
                .map(Description::toSimpleItem)
                .map(Item.class::cast)
                .toList());
        log.info("Num Child Buttons {}", items.size()).submit();

        @Nullable
        final String category = statCategory == null ? null : statCategory.getName();

        //then add the stats
        //todo
        items.addAll(container.getStats().getStatsOfPeriod(period).keySet().stream()
                .map(StatFormatters::getStatFormatter)
                .filter(keyValue -> Objects.equals(keyValue.getValue().getCategory(), category))
                .map(keyValue -> keyValue.getValue().getDescription(keyValue.getKey(), container, period))
                .map(Description::toSimpleItem)
                .map(Item.class::cast)
                .toList()
        );

        return items;
    }

    /**
     * @return The title of this menu.
     */

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Stats");
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

