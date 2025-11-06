package me.mykindos.betterpvp.core.client.stats.display;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.display.filter.PeriodFilterButton;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterButton;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public abstract class AbstractStatMenu extends AbstractGui implements IAbstractStatMenu {
    //todo abstract these to interface
    @NotNull
    private final Client client;
    @Nullable
    private final Windowed previous;
    private final StatPeriodManager statPeriodManager;

    private String periodKey;

    private final StringFilterButton<IAbstractStatMenu> periodFilterButton;

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param width  The width of the Gui
     * @param height The height of the Gui
     */
    protected AbstractStatMenu(@NotNull Client client, @Nullable Windowed previous, String periodKey, StatPeriodManager statPeriodManager) {
        super(9, 6);
        this.client = client;
        this.periodFilterButton = new PeriodFilterButton(periodKey, statPeriodManager);

        Structure baseStructure = new Structure("########P",
                "#########",
                "#########",
                "#########",
                "#########",
                "####B####")
                .addIngredient('#', Menu.BACKGROUND_GUI_ITEM)
                .addIngredient('B', new StatBackButton(previous))
                .addIngredient('P', this.periodFilterButton);
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
