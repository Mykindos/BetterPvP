package me.mykindos.betterpvp.core.recipe.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GuiRecipeList extends AbstractPagedGui<BaseItem> {

    private static final URL url;

    static {
        try {
            url = URI.create("https://wiki.betterpvp.net/").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private final ItemFactory itemFactory;
    private final RecipeRegistries registries;

    public GuiRecipeList(ItemFactory itemFactory, RecipeRegistries registries) {
        super(9, 6, false, new Structure(
                "000000000",
                "0XXXXXXX0",
                "<XXXXXXX>",
                "0XXXXXXX0",
                "000000000",
                "00000000I"
        ).addIngredient('X', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', PageBackwardButton.invisible())
                .addIngredient('>', PageForwardButton.invisible())
                .addIngredient('I', InfoTabButton.builder()
                        // todo: wiki entry
                        .wikiEntry("Test", url)
                        .description(Component.text("Most items have a recipe they can be obtained with. The anvil, workbench, smelter and imbuement pedestal all make use of recipes listed here."))
                        .build()));
        this.itemFactory = itemFactory;
        this.registries = registries;
        refresh();
    }

    public void refresh() {
        List<BaseItem> content = new ArrayList<>();
        for (Recipe<?, ?> recipe : registries.getRecipes()) {
            if (recipe.getPrimaryResult() instanceof BaseItem item) {
                content.add(item);
            }
        }

        setContent(content);
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (BaseItem item : content) {
            page.add(new SlotElement.ItemSlotElement(new ItemButton(item)));

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

    private class ItemButton extends AbstractItem {

        private final BaseItem item;

        private ItemButton(BaseItem item) {
            this.item = item;
        }

        @Override
        public ItemProvider getItemProvider() {
            final ItemInstance itemInstance = itemFactory.create(item);
            return ItemView.of(itemInstance.getView().get()).toBuilder()
                    .action(ClickActions.ALL, Component.text("View Recipes"))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            // todo: open GuiRecipeViewer
        }
    }
}
