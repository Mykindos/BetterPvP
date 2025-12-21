package me.mykindos.betterpvp.core.client.stats.display;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.filter.RealmFilterButton;
import me.mykindos.betterpvp.core.client.stats.display.filter.SeasonFilterButton;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.server.Period;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public abstract class AbstractStatMenu extends AbstractGui implements IAbstractStatMenu {
    @NotNull
    private final Client client;
    @Nullable
    private final Windowed previous;
    private final RealmManager realmManager;

    private final SeasonFilterButton seasonFilterButton;
    private final RealmFilterButton realmFilterButton;

    private StatFilterType type;
    private Period period;

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param width  The width of the Gui
     * @param height The height of the Gui
     */
    protected AbstractStatMenu(@NotNull Client client, @Nullable Windowed previous, StatFilterType type, @Nullable Period period, RealmManager realmManager) {
        super(9, 6);
        this.seasonFilterButton = new SeasonFilterButton(
                IAbstractStatMenu.getSeasonContext(type, period),
                IAbstractStatMenu.getSeasonContexts(realmManager));
        this.realmFilterButton = new RealmFilterButton(
                IAbstractStatMenu.getRealmContext(type, period),
                IAbstractStatMenu.getRealmContexts(seasonFilterButton.getSelectedFilter().getSeason(), realmManager));


        Structure baseStructure = new Structure(
                "# # # # # # # S R",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#',Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PageBackwardButton())
                .addIngredient('-', new StatBackButton(previous))
                .addIngredient('>', new PageForwardButton())
                .addIngredient('S', seasonFilterButton)
                .addIngredient('R', realmFilterButton);

        applyStructure(baseStructure);

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
}
