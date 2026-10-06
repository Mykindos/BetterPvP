package me.mykindos.betterpvp.core.logging.menu.button;

import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.menu.button.filter.FilterButton;
import me.mykindos.betterpvp.core.menu.button.filter.StringContext;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StringFilterButton<G extends Gui> extends FilterButton<G, StringContext> {

    /**
     * Create a String filter button, that goes through
     * All values of context, with no default set
     * @param title the title of this button
     * @param numToShow the number of options to show at once
     * @param displayMaterial the material of the item
     */
    public StringFilterButton(String title, int numToShow, Material displayMaterial) {
        this(title, new ArrayList<>(List.of("All")), numToShow, displayMaterial);
    }

    /**
     * Create a String filter button, that goes through
     * All values of context, with no default set
     * @param title the title of this button
     * @param contexts the list of contexts to use
     * @param numToShow the number of options to show at once
     * @param displayMaterial the material of the item
     */
    public StringFilterButton(String title, List<String> contexts, int numToShow, Material displayMaterial) {
        super(title, contexts.stream().map(StringContext::new).toList(), numToShow, displayMaterial);
    }


    public void add(@NotNull String newFilter) {
        final StringContext stringContext = new StringContext(newFilter);
        this.add(stringContext);
    }
    public void setSelectedFilter(String filter) {
        final StringContext stringContext = new StringContext(filter);
        this.setSelectedFilter(stringContext);
    }


}
