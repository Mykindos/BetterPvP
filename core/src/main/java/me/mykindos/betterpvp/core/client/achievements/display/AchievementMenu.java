package me.mykindos.betterpvp.core.client.achievements.display;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.display.button.AchievementCategoryButton;
import me.mykindos.betterpvp.core.client.achievements.display.button.AchievementPeriodFilterButton;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.PeriodFilterButton;
import me.mykindos.betterpvp.core.client.stats.display.StatBackButton;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterButton;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@CustomLog
@Getter
@Setter
public class AchievementMenu extends AbstractPagedGui<Item> implements IAbstractStatMenu {
    private final AchievementManager achievementManager;
    private final StatPeriodManager statPeriodManager;
    private final IAchievementCategory achievementCategory;
    private final Windowed previous;
    private final Client client;

    private final StringFilterButton<IAbstractStatMenu> periodFilterButton;

    private String periodKey;

    public AchievementMenu(Client client, AchievementManager achievementManager, StatPeriodManager statPeriodManager) {
        this(client, null, StatContainer.GLOBAL_PERIOD_KEY, achievementManager, statPeriodManager, null);
    }

    public AchievementMenu(Client client, @Nullable IAchievementCategory achievementCategory, String periodKey, AchievementManager achievementManager, StatPeriodManager statPeriodManager, @Nullable Windowed previous) {
        super(9, 5, false,
                new Structure("# # # # # # # # P",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# # # < - > # # #")
                        .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                        .addIngredient('#', Menu.BACKGROUND_ITEM)
                        .addIngredient('P', new PeriodFilterButton(periodKey, statPeriodManager))
                        .addIngredient('<', new PreviousButton())
                        .addIngredient('-', new StatBackButton(previous))
                        .addIngredient('>', new ForwardButton())
        );
        this.statPeriodManager = statPeriodManager;
        if (getItem(7, 0) instanceof AchievementPeriodFilterButton filterButton) {
            filterButton.setCurrent(this);
        }
        if (!(getItem(8, 0) instanceof PeriodFilterButton periodButton)) throw new IllegalStateException("Item in this slot must be a StringFilterButton");
        this.periodFilterButton = periodButton;
        this.periodFilterButton.setRefresh(() ->
            periodButton.onChangePeriod().thenApply(status -> {
                if (status.equals(Boolean.FALSE)) return Boolean.FALSE;
                setContent(getItems());
                return Boolean.TRUE;
            })
        );
        this.client = client;
        this.achievementManager = achievementManager;
        this.achievementCategory = achievementCategory;
        this.previous = previous;
        this.periodKey = periodKey;
        setContent(getItems());
    }

    private List<Item> getItems() {
        Collection<IAchievementCategory> childCategories;
        //reset the achievement category if it is no longer valid
        //get the category buttons
        if (achievementCategory != null) {
            childCategories = achievementCategory.getChildren();
        } else {
            childCategories = achievementManager.getAchievementCategoryManager().getObjects().values().stream()
                    .filter(category -> category.getParent() == null)
                    .toList();
        }

        //add the categories
        final List<Item> items = new ArrayList<>(childCategories.stream()
                .map(child -> (Item) new AchievementCategoryButton(child, periodKey, client, achievementManager, statPeriodManager, this))
                .toList());

        @Nullable
        final NamespacedKey category = achievementCategory == null ? null : achievementCategory.getNamespacedKey();

        //then add the achievements
        items.addAll(achievementManager.getObjects().values()
                .stream()
                .filter(achievement -> Objects.equals(achievement.getAchievementCategory(), category))
                .filter(achievement -> {
                               if (Objects.equals(periodKey, StatContainer.GLOBAL_PERIOD_KEY)) {
                                   return achievement.getAchievementType() == AchievementType.GLOBAL;
                               }
                               return achievement.getAchievementType() == AchievementType.PERIOD;
                })
                .sorted(Comparator.comparingInt(achievement -> achievement.getPriority(client.getStatContainer(), periodKey)))
                .map(achievement -> {
                    try {
                        return (Item) achievement.getDescription(client.getStatContainer(), periodKey).toSimpleItem();
                    } catch (Exception e) {
                        log.error("Error getting description for Achievement {} ({}) ", achievement.getName(), achievement.getNamespacedKey().asString(), e).submit();
                    }
                    return new SimpleItem(ItemProvider.EMPTY);
                })
                .toList()
        );

        return items;
    }

    /**
     * @return The title of this menu.
     */

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Achievements");
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

