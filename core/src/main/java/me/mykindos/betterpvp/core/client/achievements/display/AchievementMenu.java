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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    @NotNull
    private StatFilterType type;
    private Period period;

    public AchievementMenu(Client client, AchievementManager achievementManager, RealmManager realmManager) {
        this(client, null, StatFilterType.ALL, null, achievementManager, realmManager, null);
    }

    public AchievementMenu(@NotNull Client client, @Nullable IAchievementCategory achievementCategory, @NotNull StatFilterType type, @Nullable Period period, AchievementManager achievementManager, RealmManager realmManager, @Nullable Windowed previous) {
        super(9, 6, false, new Structure(
                "# # # # # # # S R",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PageBackwardButton())
                .addIngredient('-', new StatBackButton(previous))
                .addIngredient('>', new PageForwardButton())
                //todo fix these monstrosities
                .addIngredient('S', new SeasonFilterButton(
                        IAbstractStatMenu.getSeasonContext(type, period),
                        IAbstractStatMenu.getSeasonContexts(realmManager)))
                .addIngredient('R', new RealmFilterButton(
                        IAbstractStatMenu.getRealmContext(type, period),
                        IAbstractStatMenu.getRealmContexts(Objects.requireNonNull(IAbstractStatMenu.getSeasonContext(type, period)).getSeason(), realmManager)))
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

        this.type = type;
        this.period = type == StatFilterType.ALL ? null : period;

        if (type == StatFilterType.ALL) {
            // Global achievements are not season/realm scoped — hide both controls.
            setItem(7, 0, Menu.BACKGROUND_GUI_ITEM);
            setItem(8, 0, Menu.BACKGROUND_GUI_ITEM);
        } else {
            // Seasonal: remove the "All" season option and hide the realm filter entirely.
            seasonButton.getContexts().removeIf(ctx -> ctx.getStatFilterType() == StatFilterType.ALL);
            setItem(8, 0, Menu.BACKGROUND_GUI_ITEM);

            seasonButton.setRefresh(() ->
                    seasonButton.onChangeSeason().thenApply(status -> {
                        if (status.equals(Boolean.FALSE)) return Boolean.FALSE;
                        setContent(getItems());
                        return Boolean.TRUE;
                    })
            );
        }

        setContent(getItems());
    }

    private List<Item> getItems() {
        Collection<IAchievementCategory> childCategories;
        if (achievementCategory != null) {
            childCategories = achievementCategory.getChildren();
        } else {
            childCategories = achievementManager.getAchievementCategoryManager().getObjects().values().stream()
                    .filter(category -> category.getParent() == null)
                    .toList();
        }

        final Set<NamespacedKey> validCategoryTree = getValidCategoryTree();

        //add the categories (only those with at least one matching achievement in their tree)
        final List<Item> items = new ArrayList<>(childCategories.stream()
                .filter(child -> validCategoryTree.contains(child.getNamespacedKey()))
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

    private Set<NamespacedKey> getValidCategoryTree() {
        final Set<NamespacedKey> validCategories = new HashSet<>();
        achievementManager.getAchievementCategoryManager().getObjects().values()
                .forEach(category -> markValidCategoryTree(category, validCategories));
        return validCategories;
    }

    private boolean markValidCategoryTree(@NotNull IAchievementCategory category, @NotNull Set<NamespacedKey> validCategories) {
        final NamespacedKey categoryKey = category.getNamespacedKey();
        final boolean hasDirectAchievements = achievementManager.getObjects().values().stream()
                .anyMatch(achievement -> type.equals(achievement.getAchievementFilterType())
                        && Objects.equals(achievement.getAchievementCategory(), categoryKey));

        boolean hasValidChild = false;
        for (IAchievementCategory child : category.getChildren()) {
            if (markValidCategoryTree(child, validCategories)) {
                hasValidChild = true;
            }
        }

        if (hasDirectAchievements || hasValidChild) {
            validCategories.add(categoryKey);
            return true;
        }

        return false;
    }

    /**
     * @return The title of this menu.
     */

    @Override
    public @NotNull Component getTitle() {
        return Component.text(type == StatFilterType.ALL ? "Achievements - Global" : "Achievements - Seasonal");
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
