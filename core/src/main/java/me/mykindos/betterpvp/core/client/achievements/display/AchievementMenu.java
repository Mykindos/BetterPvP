package me.mykindos.betterpvp.core.client.achievements.display;

import java.util.ArrayList;
import java.util.List;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.AchievementManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

//TODO this is a sub menu
public class AchievementMenu extends AbstractPagedGui<Description> implements Windowed {
    private final AchievementManager achievementManager;
    private final Client client;
    private final Gamer gamer;
    private Showing current;

    protected AchievementMenu(Client client, AchievementManager achievementManager) {
        super(9, 5, false, new Structure("C G # # # # # # #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #"));
                //.addIngredient('C',));
        this.client = client;
        this.gamer = client.getGamer();
        this.current = Showing.CLIENT;
        this.achievementManager = achievementManager;


/*

        List<Description> items = achievementManager.getObjects().values()
                .stream()
                .filter(achievement -> achievement.getClass().getTypeParameters()[0].getClass().isAssignableFrom(container.getClass()))
                .map(achievement -> achievement.getDescription(container))
                .toList();*/
    }

    /**
     * @return The title of this menu.
     */

    @Override
    public @NotNull Component getTitle() {
        return null;
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

    static enum Showing {
        CLIENT,
        GAMER
    }
}

