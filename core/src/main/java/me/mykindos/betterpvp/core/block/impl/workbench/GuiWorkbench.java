package me.mykindos.betterpvp.core.block.impl.workbench;

import lombok.NonNull;
import me.mykindos.betterpvp.core.inventory.gui.AbstractTabGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.resolver.HasIngredientsParameter;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class GuiWorkbench extends AbstractTabGui implements Windowed {

    protected final GuiCraftingTableAdvanced craftingGui;
    protected final GuiQuickCraftViewer quickCraftGui;
    private final List<List<SlotElement>> linkingElements;
    private final CraftingManager craftingManager;
    protected final ItemFactory itemFactory;
    protected LinkedList<CraftingRecipe> quickCrafts = new LinkedList<>();
    protected final HasIngredientsParameter lookupParameter;
    private Window window;
    private final WeakReference<Player> playerRef;

    public GuiWorkbench(Player player, CraftingManager craftingManager, ItemFactory itemFactory) {
        super(9, 6, 2, new Structure(
                "xxxxxxxxx",
                "xxxxxxxxx",
                "xxxxxxxxx",
                "xxxxxxxxx",
                "xxxxxxxxx",
                "xxxxxxxxx"
        ).addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL));

        this.playerRef = new WeakReference<>(player);
        this.craftingManager = craftingManager;
        this.itemFactory = itemFactory;
        this.craftingGui = new GuiCraftingTableAdvanced(this, craftingManager, itemFactory);
        this.quickCraftGui = new GuiQuickCraftViewer(this);
        this.linkingElements = List.of(
                getLinkingElements(craftingGui),
                getLinkingElements(quickCraftGui)
        );
        this.lookupParameter = new HasIngredientsParameter(player, itemFactory);
        setTab(0);
        updateQuickCrafts();
    }

    public void updateQuickCrafts() {
        this.quickCrafts = this.craftingManager.getRegistry()
                .getResolver()
                .lookup(lookupParameter);
        this.craftingGui.updateControlItems();
        this.quickCraftGui.refresh();
    }

    public void setCraftingTab() {
        this.setTab(0);
    }

    public void setQuickCraftTab() {
        this.setTab(1);
    }

    private Component getCurrentTitle() {
        return switch (this.getCurrentTab()) {
            case 0 -> Component.text("<shift:-48><glyph:menu_workbench>").font(NEXO);
            case 1 -> Component.text("<shift:-48><glyph:menu_quick_craft_viewer>").font(NEXO);
            default -> throw new IllegalStateException("Unexpected value: " + this.getCurrentTab());
        };
    }

    @Override
    public void setTab(int tab) {
        super.setTab(tab);
        if (window != null) {
            window.changeTitle(getCurrentTitle());
        }
        final Player player = this.playerRef.get();
        if (player != null) {
            new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f).play(player);
        }
    }

    @Override
    public boolean isTabAvailable(int tab) {
        return true;
    }

    @Override
    public @NotNull List<Gui> getTabs() {
        return List.of(this.craftingGui, this.quickCraftGui);
    }

    @Override
    protected List<SlotElement> getSlotElements(int tab) {
        return this.linkingElements.get(tab);
    }

    private List<SlotElement> getLinkingElements(Gui gui) {
        if (gui == null) return null;

        List<SlotElement> elements = new ArrayList<>();
        for (int slot = 0; slot < gui.getSize(); slot++) {
            SlotElement link = new SlotElement.LinkedSlotElement(gui, slot);
            elements.add(link);
        }

        return elements;
    }

    @Override
    public Window show(@NonNull Player player) {
        final Window window = Windowed.super.show(player);
        window.addCloseHandler(() -> craftingGui.refund(player));
        this.window = window;
        return window;
    }

    @Override
    public @NotNull Component getTitle() {
        return getCurrentTitle();
    }
}
