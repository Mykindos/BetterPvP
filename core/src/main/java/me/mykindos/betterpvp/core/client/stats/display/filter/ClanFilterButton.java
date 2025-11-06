package me.mykindos.betterpvp.core.client.stats.display.filter;

import me.mykindos.betterpvp.core.client.stats.display.clans.ClansStatMenu;
import me.mykindos.betterpvp.core.menu.button.filter.FilterButton;
import org.bukkit.Material;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClanFilterButton extends FilterButton<ClansStatMenu, ClanContext> {
    /**
     * Create a String filter button, that goes through
     * All values of context, with no default set
     *
     * @param title           the title of this button
     * @param contexts        the list of contexts to use
     * @param numToShow       the number of options to show at once
     * @param displayMaterial the material of the item
     * @param customModelData the custom model data of the item
     */
    public ClanFilterButton(String title, List<ClanContext> contexts, int numToShow, Material displayMaterial, int customModelData) {
        super(title, contexts, numToShow, displayMaterial, customModelData);
    }

    public CompletableFuture<Boolean> onChangeClan() {
        this.getGui().setClanContext(this.getSelectedFilter());
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }
}
