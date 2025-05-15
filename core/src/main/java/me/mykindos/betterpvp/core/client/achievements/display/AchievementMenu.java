package me.mykindos.betterpvp.core.client.achievements.display;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.AchievementManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

@CustomLog
public class AchievementMenu extends AbstractPagedGui<Description> implements Windowed {
    private final AchievementManager achievementManager;
    private final Client client;
    private final Gamer gamer;
    @Getter
    @Setter
    private Showing current;

    public AchievementMenu(Client client, AchievementManager achievementManager) {
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
                .addIngredient('-', new BackButton(null))
                .addIngredient('>', new ForwardButton())
        );
        this.client = client;
        this.gamer = client.getGamer();
        this.achievementManager = achievementManager;
        this.setCurrent(Showing.CLIENT);
    }

    private List<Description> getItems() {
        PropertyContainer propertyContainer = this.current == Showing.CLIENT ? this.client : this.gamer;
        List<Description> items = new ArrayList<>(achievementManager.getObjects().values()
                .stream()
                .filter(achievement -> achievement.isSameType(propertyContainer))
                .map(achievement -> achievement.getDescription(propertyContainer))
                .toList());
        log.info(items.toString()).submit();
        return items;
    }

    public void setCurrent(Showing current) {
        this.current = current;
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

        for (Description item : content) {
            page.add(new SlotElement.ItemSlotElement(item.toSimpleItem()));

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

    @Getter
    @AllArgsConstructor
    public enum Showing {
        CLIENT("Client", Material.PLAYER_HEAD),
        GAMER("Gamer", Material.IRON_SWORD);
        private final String name;
        private final Material material;
    }
}

