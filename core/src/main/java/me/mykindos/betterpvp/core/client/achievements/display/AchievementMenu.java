package me.mykindos.betterpvp.core.client.achievements.display;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.display.button.AchievementCategoryButton;
import me.mykindos.betterpvp.core.client.achievements.display.button.PropertyContainerButton;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
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
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CustomLog
public class AchievementMenu extends AbstractPagedGui<Item> implements Windowed {
    private final AchievementManager achievementManager;
    private IAchievementCategory achievementCategory;
    private final Client client;
    private final Gamer gamer;
    @Getter
    private Showing current;

    public AchievementMenu(Client client, AchievementManager achievementManager) {
        this(client, achievementManager, Showing.CLIENT, null, null);
    }

    public AchievementMenu(Client client, AchievementManager achievementManager, Showing current, @Nullable IAchievementCategory achievementCategory, @Nullable Windowed previous) {
        super(9, 5, false, new Structure("C G # # # # # # #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #")
                .addIngredient('C', new PropertyContainerButton(Showing.CLIENT))
                .addIngredient('G', new PropertyContainerButton(Showing.GAMER))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PreviousButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new ForwardButton())
        );
        this.client = client;
        this.gamer = client.getGamer();
        this.achievementManager = achievementManager;
        this.achievementCategory = achievementCategory;
        this.current = current;
        setContent(getItems());
    }

    private List<Item> getItems() {
        final PropertyContainer propertyContainer = this.current == Showing.CLIENT ? this.client : this.gamer;
        Collection<IAchievementCategory> childCategories;
        //reset the achievement category if it is no longer valid
        if (achievementCategory != null && !achievementCategory.isAllowed(propertyContainer)) {
            log.info("Invalid parent category").submit();
            achievementCategory = null;
        }
        //get the category buttons
        if (achievementCategory != null) {
            childCategories = achievementCategory.getChildren();
        } else {
            log.info(achievementManager.getAchievementCategoryManager().toString()).submit();
            childCategories = achievementManager.getAchievementCategoryManager().getObjects().values().stream()
                    .filter(category -> category.getParent() == null)
                    .toList();
        }


        log.info("Num Child Categories {}", childCategories.size()).submit();

        //add the categories
        final List<Item> items = new ArrayList<>(childCategories.stream()
                .filter(child -> child.isAllowed(propertyContainer))
                .map(child -> (Item) new AchievementCategoryButton(child, client, achievementManager, this.current, this))
                .toList());
        log.info("Num Child Buttons {}", items.size()).submit();

        @Nullable
        final NamespacedKey category = achievementCategory == null ? null : achievementCategory.getNamespacedKey();

        //then add the achievements
        items.addAll(achievementManager.getObjects().values()
                .stream()
                .filter(achievement -> achievement.isSameType(propertyContainer))
                .filter(achievement -> Objects.equals(achievement.getAchievementCategory(), category))
                .map(achievement -> (Item) achievement.getDescription(propertyContainer).toSimpleItem())
                .toList()
        );

        return items;
    }

    public void setCurrent(Showing current) {
        this.current = current;
        this.achievementCategory = null;
        setContent(getItems());
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

