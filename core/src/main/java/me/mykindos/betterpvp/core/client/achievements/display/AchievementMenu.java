package me.mykindos.betterpvp.core.client.achievements.display;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.display.button.AchievementCategoryButton;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatBackButton;
import me.mykindos.betterpvp.core.client.stats.display.filter.RealmFilterButton;
import me.mykindos.betterpvp.core.client.stats.display.filter.SeasonFilterButton;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.server.Period;
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
    private final IAchievementCategory achievementCategory;
    @NotNull
    private final Client client;
    @Nullable
    private final Windowed previous;
    private final RealmManager realmManager;

    private final SeasonFilterButton seasonFilterButton;
    private final RealmFilterButton realmFilterButton;

    private StatFilterType type;
    private Period period;

    public AchievementMenu(Client client, AchievementManager achievementManager, RealmManager realmManager) {
        this(client, null, StatFilterType.ALL, null, achievementManager, realmManager, null);
    }

    public AchievementMenu(@NotNull Client client, @Nullable IAchievementCategory achievementCategory, StatFilterType type, Period period, AchievementManager achievementManager, RealmManager realmManager, @Nullable Windowed previous) {
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
        this.realmManager = realmManager;

        this.client = client;
        this.achievementManager = achievementManager;
        this.achievementCategory = achievementCategory;
        this.previous = previous;
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
                .map(child -> (Item) new AchievementCategoryButton(child, type, period, client, achievementManager, realmManager, this))
                .toList());

        @Nullable
        final NamespacedKey category = achievementCategory == null ? null : achievementCategory.getNamespacedKey();

        //then add the achievements
        items.addAll(achievementManager.getObjects().values()
                .stream()
                .filter(achievement -> Objects.equals(achievement.getAchievementCategory(), category))
                .filter(achievement -> type.equals(achievement.getAchievementFilterType()))
                .sorted(Comparator.comparingInt(achievement -> achievement.getPriority(client.getStatContainer(), type, period)))
                .map(achievement -> {
                    try {
                        return (Item) achievement.getDescription(client.getStatContainer(), type, period).toSimpleItem();
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

