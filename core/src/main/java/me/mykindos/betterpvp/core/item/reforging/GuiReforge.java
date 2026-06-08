package me.mykindos.betterpvp.core.item.reforging;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.item.builder.ItemBuilder;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.currency.CurrencyComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatAugmentationComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBiasRegistry;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.menu.button.PlaceholderInventorySlot;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

/**
 * GUI for reforging items to augment their statistics.
 */
public class GuiReforge extends AbstractGui implements Windowed {

    private final VirtualInventory goldInventory;
    private final VirtualInventory powderInventory;
    private final VirtualInventory itemInventory;
    private final Player player;
    private final ItemFactory itemFactory;

    public GuiReforge(Player player, ItemFactory itemFactory, PurityReforgeBiasRegistry biasRegistry) {
        super(9, 3);
        this.player = player;
        this.itemFactory = itemFactory;

        // Create VirtualInventories with placeholders
        this.goldInventory = new VirtualInventory(UUID.randomUUID(), new ItemStack[1]);
        this.powderInventory = new VirtualInventory(UUID.randomUUID(), new ItemStack[1]);
        this.itemInventory = new VirtualInventory(UUID.randomUUID(), new ItemStack[1]);
        this.itemInventory.setGuiPriority(-Integer.MAX_VALUE); // We want people shift-clicking items last into here

        // Set up inventory handlers
        setupInventoryHandler(goldInventory, instance -> instance.getComponent(CurrencyComponent.class).isPresent());
        setupInventoryHandler(powderInventory, instance -> instance.getComponent(StatAugmentationComponent.class).isPresent());
        setupInventoryHandler(itemInventory, instance -> instance.getComponent(StatContainerComponent.class).isPresent());

        // Create button instances
        ReforgingButton primaryButton = new ReforgingButton(itemFactory, goldInventory, powderInventory, itemInventory, biasRegistry, true);
        ReforgingButton secondaryButton = new ReforgingButton(itemFactory, goldInventory, powderInventory, itemInventory, biasRegistry, false);

        // Apply structure
        applyStructure(new Structure(
                "00000000I",
                "0GS0T0BC0",
                "000000000")
                .addIngredient('0', Menu.INVISIBLE_BACKGROUND_ITEM)
                .addIngredient('I', InfoTabButton.builder()
                        .descriptionLines(List.of(Translations.rawComponentLines("core.menu.reforge.info.description")))
                        .icon(ItemStack.of(Material.AIR))
                        .build())
                .addIngredient('G', new PlaceholderInventorySlot(goldInventory, new ItemBuilder(createGoldPlaceholder())))
                .addIngredient('S', new PlaceholderInventorySlot(powderInventory, new ItemBuilder(createPowderPlaceholder())))
                .addIngredient('T', new PlaceholderInventorySlot(itemInventory, new ItemBuilder(createItemPlaceholder())))
                .addIngredient('B', primaryButton)
                .addIngredient('C', secondaryButton));
    }

    /**
     * Sets up a pre-update handler for a VirtualInventory that validates items
     *
     * @param inventory The VirtualInventory to configure
     * @param validator Predicate to validate if an ItemInstance is acceptable
     */
    private void setupInventoryHandler(VirtualInventory inventory, Predicate<ItemInstance> validator) {
        inventory.setPreUpdateHandler(event -> {
            final ItemStack newItem = event.getNewItem();

            final Optional<ItemInstance> newItemOpt = newItem == null ? Optional.empty() : itemFactory.fromItemStack(newItem);
            if (newItemOpt.isEmpty()) {
                return;
            }

            final ItemInstance instance = newItemOpt.get();
            if (!validator.test(instance)) {
                event.setCancelled(true);
            }
        });

        inventory.setPostUpdateHandler(event -> {
            // Update the action button in the GUI
            this.updateControlItems();
        });
    }

    /**
     * Creates the gold placeholder ItemStack
     */
    private ItemStack createGoldPlaceholder() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .displayName(Translations.component("core.menu.reforge.gold.name").color(NamedTextColor.GOLD))
                .lore(List.of(Translations.componentLines("core.menu.reforge.gold.lore")))
                .build()
                .get();
    }

    /**
     * Creates the stat powder placeholder ItemStack
     */
    private ItemStack createPowderPlaceholder() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .displayName(Translations.component("core.menu.reforge.powder.name").color(NamedTextColor.LIGHT_PURPLE)
                        .appendSpace()
                        .append(Translations.component("core.menu.reforge.powder.optional").color(NamedTextColor.RED)))
                .lore(List.of(Translations.componentLines("core.menu.reforge.powder.lore")))
                .build()
                .get();
    }

    /**
     * Create item placeholder
     */
    private ItemStack createItemPlaceholder() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .displayName(Translations.component("core.menu.reforge.item.name").color(NamedTextColor.GREEN))
                .lore(List.of(Translations.componentLines("core.menu.reforge.item.lore")))
                .build()
                .get();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-8><glyph:menu_reforge>").font(NEXO);
    }

    @Override
    public Window show(@NotNull Player player) {
        Window window = Windowed.super.show(player);
        window.addCloseHandler(() -> refundAllItems(player));
        return window;
    }

    /**
     * Returns all items in the virtual inventories to the player when GUI closes
     */
    private void refundAllItems(Player player) {
        // Refund gold
        ItemStack gold = goldInventory.getItem(0);
        UtilItem.insert(player, gold);

        // Refund powder
        ItemStack powder = powderInventory.getItem(0);
        UtilItem.insert(player, powder);

        // Refund item
        ItemStack item = itemInventory.getItem(0);
        UtilItem.insert(player, item);
    }
}
