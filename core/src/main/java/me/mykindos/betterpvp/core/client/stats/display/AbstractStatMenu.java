package me.mykindos.betterpvp.core.client.stats.display;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public abstract class AbstractStatMenu extends AbstractGui implements Windowed {
    private final Client client;
    private final Windowed previous;
    private final StatPeriodManager statPeriodManager;

    private String periodKey;


    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param width  The width of the Gui
     * @param height The height of the Gui
     */
    protected AbstractStatMenu(Client client, Windowed previous, String periodKey, StatPeriodManager statPeriodManager) {
        super(9, 6);
        this.client = client;
        Structure baseStructure = new Structure("#########",
                "#########",
                "#########",
                "#########",
                "#########",
                "####B####")
                .addIngredient('#', Menu.BACKGROUND_GUI_ITEM)
                .addIngredient('B', new BackButton(previous));
        applyStructure(baseStructure);
        this.previous = previous;
        this.periodKey = periodKey;
        this.statPeriodManager = statPeriodManager;
    }

    /**
     * @return The title of this menu.
     */
    @Override
    public @NotNull Component getTitle() {
        return Component.text(client.getName() + "'s Stats");
    }
}
