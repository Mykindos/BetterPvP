package me.mykindos.betterpvp.core.block.impl.smelter;

import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.PlayerUpdateReason;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AutoUpdateItem;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.fuel.FuelComponent;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.metal.casting.CastingMold;
import me.mykindos.betterpvp.core.recipe.smelting.LiquidAlloy;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@SuppressWarnings("ALL")
@CustomLog
public class GuiSmelter extends AbstractGui implements Windowed {

    private final SmelterData data;
    private final ItemFactory itemFactory;
    private final VirtualInventory contentInventory;
    private final VirtualInventory fuelInventory;
    private final VirtualInventory resultInventory;
    private final GuiCastingMoldPicker picker;

    @SneakyThrows
    public GuiSmelter(ItemFactory itemFactory, SmelterData data) {
        super(9, 6);
        this.data = data;
        this.itemFactory = itemFactory;
        this.picker = new GuiCastingMoldPicker(data, itemFactory, data.getCastingMoldRecipeRegistry());

        // Items to be smelted (content)
        this.contentInventory = new VirtualInventory(UUID.randomUUID(), new ItemStack[10]);
        contentInventory.setPostUpdateHandler(event -> {
            syncToStorage();
        });

        // Fuel inventory
        this.fuelInventory = new VirtualInventory(UUID.randomUUID(), new ItemStack[1]);
        fuelInventory.setPostUpdateHandler(event -> {
            final List<ItemInstance> itemInstances = itemFactory.fromArray(event.getInventory().getItems());
            data.getFuelManager().getFuelItems().setContent(itemInstances);
        });

        // Validate fuel items can only be items with FuelComponent
        fuelInventory.setPreUpdateHandler(event -> {
            if (event.getUpdateReason() instanceof PlayerUpdateReason updateReason) {
                final Player player = updateReason.getPlayer();
                final ItemStack newItem = event.getNewItem();

                // Allow removal of items (newItem is null or air)
                if (newItem == null || newItem.getType().isAir()) {
                    return;
                }

                // Check if the item has FuelComponent
                itemFactory.fromItemStack(newItem).ifPresentOrElse(
                        itemInstance -> {
                            if (itemInstance.getComponent(FuelComponent.class).isEmpty()) {
                                // Item doesn't have fuel component, cancel the update
                                event.setCancelled(true);
                                SoundEffect.WRONG_ACTION.play(player);
                            }
                        },
                        () -> {
                            // Item couldn't be converted to ItemInstance, cancel
                            event.setCancelled(true);
                            SoundEffect.WRONG_ACTION.play(player);
                        }
                );
            }
        });

        fuelInventory.setPostUpdateHandler(event -> {
            syncToStorage();
        });

        // Result inventory (casting mold slot)
        this.resultInventory = new VirtualInventory(UUID.randomUUID(), new ItemStack[1]);
        resultInventory.setPreUpdateHandler(event -> {
            if (event.getUpdateReason() instanceof PlayerUpdateReason updateReason) {
                final Player player = updateReason.getPlayer();
                final ItemStack previousItem = event.getPreviousItem();
                final ItemStack newItem = event.getNewItem();

                // Check if trying to interact with barrier (no recipe available)
                if (previousItem != null && previousItem.getType() == Material.BARRIER) {
                    // Always cancel interaction with barrier
                    event.setCancelled(true);
                    SoundEffect.WRONG_ACTION.play(player);
                    return;
                }

                // Allow taking items out (newItem is null or air)
                if (newItem == null || newItem.getType().isAir()) {
                    return; // Allow removal
                }

                // Allow taking items out when there was a previous item
                if (previousItem != null && !previousItem.getType().isAir()) {
                    if (event.getUpdateReason() instanceof PlayerUpdateReason reason) {
                        if (reason.getEvent() instanceof InventoryClickEvent clickEvent) {
                            // Allow left click to take items, but prevent other interactions
                            switch (clickEvent.getClick()) {
                                case LEFT:
                                    if (newItem == null || newItem.getType().isAir()) {
                                        // Taking the item out, allow it
                                        return;
                                    }
                                    // Trying to put something in, validate below
                                    break;
                                case SHIFT_LEFT:
                                    // Shift clicking to player inventory, allow
                                    return;
                                default:
                                    // Any other interaction, cancel
                                    event.setCancelled(true);
                                    SoundEffect.WRONG_ACTION.play(player);
                                    return;
                            }
                        }
                    }
                }

                // Validate that only empty casting molds can be placed
                itemFactory.fromItemStack(newItem).ifPresentOrElse(
                        itemInstance -> {
                            if (!(itemInstance.getBaseItem() instanceof CastingMold)) {
                                // Item is not a casting mold, cancel the update
                                event.setCancelled(true);
                                SoundEffect.WRONG_ACTION.play(player);
                            }
                            // Note: We only allow empty casting molds (CastingMold class)
                            // FullCastingMold instances are not allowed to be placed
                        },
                        () -> {
                            // Item couldn't be converted to ItemInstance, cancel
                            event.setCancelled(true);
                            SoundEffect.WRONG_ACTION.play(player);
                        }
                );
            }
        });

        resultInventory.setPostUpdateHandler(event -> {
            syncToStorage();
        });

        applyStructure(new Structure(
                "00000000I",
                "0AAA00000",
                "0AAA0W0D0",
                "00B0GggE0",
                "00C0H00F0",
                "000000000")
                // Content inventory
                .addIngredient('A', contentInventory)
                // Fuel meter
                .addIngredient('B', new FuelMeter())
                // Fuel inventory
                .addIngredient('C', fuelInventory)
                // Casting mold slot
                .addIngredient('D', new CastingMoldPicker())
                // Results (liquid and casting molds)
                .addIngredient('E', new AlloyStorage())
                .addIngredient('F', resultInventory)
                .addIngredient('G', new ProgressArrow(false))
                .addIngredient('g', new ProgressArrow(true))
                .addIngredient('W', new WarningIcon())
                .addIngredient('I', InfoTabButton.builder()
                        .description(Component.text("The smelter allows you to melt down metals and " +
                                "alloys into liquid form, which can then be cast into various shapes using casting molds." +
                                " It requires fuel to operate and has a temperature system that affects the smelting process."))
                        // todo: wiki entry
                        .wikiEntry("", URI.create("https://wiki.betterpvp.net/").toURL())
                        .build())
        );
    }

    protected void syncFromStorage() {
        // Update content items
        syncFromStorage(contentInventory, data.getProcessingEngine().getContentItems().getContent());
        // Update fuel items
        syncFromStorage(fuelInventory, data.getFuelManager().getFuelItems().getContent());
        // Update result items or show barrier if no recipe available
        syncResultInventory();
        updateControlItems();
    }

    private void syncResultInventory() {
        if (!data.getProcessingEngine().getResultItems().isEmpty()) {
            // We have a valid recipe, sync normally
            syncFromStorage(resultInventory, data.getProcessingEngine().getResultItems().getContent());
        }
    }

    protected void syncToStorage() {
        // Sync content items
        data.getProcessingEngine().getContentItems().setContent(itemFactory.fromArray(contentInventory.getItems()));
        // Sync fuel items
        data.getFuelManager().getFuelItems().setContent(itemFactory.fromArray(fuelInventory.getItems()));
        // Sync result items
        if (resultInventory.getItems()[0] == null || resultInventory.getItems()[0].getType() == Material.BARRIER) {
            // If the first item is a barrier, we don't sync it
            data.getProcessingEngine().getResultItems().setContent(List.of());
            return;
        }
        data.getProcessingEngine().getResultItems().setContent(itemFactory.fromArray(resultInventory.getItems()));
    }

    private void syncFromStorage(VirtualInventory inventory, List<ItemInstance> items) {
        for (int i = 0; i < inventory.getUnsafeItems().length; i++) {
            inventory.setItemSilently(i, null); // Clear existing items
        }
        for (int i = 0; i < items.size(); i++) {
            ItemInstance item = items.get(i);
            final ItemStack stack = item == null ? null : item.createItemStack();
            inventory.setItemSilently(i, stack);
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_smelter>").font(NEXO);
    }

    private class FuelMeter extends AutoUpdateItem {

        private FuelMeter() {
            super(1, null); // we can use null here because we override getItemProvider
        }

        private Component getFuelComponent() {
            final float percentage = Math.max(0, Math.min(1, data.getBurnTime() / (float) data.getFuelManager().getLastBurnTime()));
            final ProgressColor progressColor = new ProgressColor(percentage);
            return Component.text("Burn Time:", TextColor.color(214, 214, 214))
                    .appendSpace()
                    .append(Component.text((int) Math.ceil(data.getBurnTime() / 1000.) + "s", progressColor.getTextColor()));
        }

        private Component getTemperatureComponent() {
            final float temperature = data.getTemperature();
            final TextColor color = new ProgressColor(temperature / 1_000).inverted().getTextColor();
            return Component.text("Temperature:", TextColor.color(214, 214, 214))
                    .appendSpace()
                    .append(Component.text((int) temperature + " °C", color));
        }

        @Override
        public ItemProvider getItemProvider() {
            if (!data.isBurning()) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Resources.ItemModel.INVISIBLE)
                        .displayName(getFuelComponent())
                        .lore(getTemperatureComponent())
                        .build();
            }

            final float percentage =  data.getBurnTime() / (float) data.getFuelManager().getLastBurnTime();
            final int phase = 12 - Math.max(0, Math.min(12, (int) Math.round(percentage * 12))); // 13 available phases (0-based)
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/sprite/smelter/fuel_indicator"))
                    .customModelData(phase)
                    .displayName(getFuelComponent())
                    .lore(getTemperatureComponent())
                    .build();
        }
    }

    private class ProgressArrow extends AutoUpdateItem {

        private final boolean invisible;

        private ProgressArrow(boolean invisible) {
            super(1, null); // we can use null here because we override getItemProvider
            this.invisible = invisible;
        }

        @Override
        public ItemProvider getItemProvider() {
            final float percentage = data.getProcessingEngine().getSmeltingProgress();
            final int phase = Math.max(0, Math.min(14, Math.round(percentage * 14))); // 15 available phases
            if (phase == 0 || data.getProcessingEngine().getSmeltingAlloy() == null) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Resources.ItemModel.INVISIBLE)
                        .hideTooltip(true)
                        .build();
            }
            final int seconds = (int) (Math.ceil(data.getProcessingEngine().getSmeltingTime() * (1 - percentage) / 1000));
            final ProgressColor progressColor = new ProgressColor(percentage);
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(invisible ? Resources.ItemModel.INVISIBLE : Key.key("betterpvp", "menu/sprite/smelter/progress_indicator"))
                    .customModelData(phase)
                    .displayName(Component.text(seconds + "s", progressColor.getTextColor()))
                    .build();
        }

    }

    private class AlloyStorage extends AutoUpdateItem {

        private AlloyStorage() {
            super(1, null); // we can use null here because we override getItemProvider
        }

        @Override
        public ItemProvider getItemProvider() {
            final LiquidAlloy storedLiquid = data.getLiquidManager().getStoredLiquid();
            final int millibuckets = storedLiquid == null ? 0 : storedLiquid.getMillibuckets();
            final int maxMillibuckets = data.getMaxLiquidCapacity();
            final TextColor storedColor = new ProgressColor(millibuckets / (float) maxMillibuckets).inverted().getTextColor();
            final ItemView.ItemViewBuilder builder = ItemView.builder();
            builder.material(Material.PAPER);
            builder.lore(Component.text("Stored: ", TextColor.color(214, 214, 214))
                            .appendSpace()
                            .append(Component.text(millibuckets + " mB", storedColor)));
            builder.lore(Component.text("Max Capacity: ", TextColor.color(214, 214, 214))
                            .appendSpace()
                            .append(Component.text(maxMillibuckets + " mB", TextColor.color(255, 0, 0))));
            builder.action(ClickActions.RIGHT_SHIFT, Component.text("Clear", NamedTextColor.RED));

            final float progress = millibuckets / (float) maxMillibuckets;
            final int phase = Math.max(0, Math.min(15, Math.round(progress * 15))); // 16 available phases
            if (storedLiquid == null || phase == 0) {
                return builder
                        .itemModel(Resources.ItemModel.INVISIBLE)
                        .displayName(Component.text("Empty", TextColor.color(255, 86, 74)))
                        .build();
            }

            final TextColor color = TextColor.color(storedLiquid.getAlloyType().getColor().asRGB());
            final TextComponent name = Component.text(storedLiquid.getName(), color, TextDecoration.BOLD);
            return builder.itemModel(Key.key("betterpvp", "menu/sprite/smelter/alloy_indicator/" + storedLiquid.getAlloyType().getTextureKey()))
                    .displayName(name)
                    .customModelData(phase)
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType != ClickType.SHIFT_RIGHT) {
                return;
            }

            data.getLiquidManager().setStoredLiquid(null);
            SoundEffect.LOW_PITCH_PLING.play(player);
        }
    }

    private class CastingMoldPicker extends ControlItem<GuiSmelter> {

        @Override
        public ItemProvider getItemProvider(GuiSmelter gui) {
            final CastingMold castingMold = data.getProcessingEngine().getCastingMold();
            if (castingMold == null) {
                return ItemView.builder()
                        .material(Material.BARRIER)
                        .itemModel(Resources.ItemModel.STOP)
                        .displayName(Component.text("No Casting Mold Selected", NamedTextColor.RED))
                        .action(ClickActions.ALL, Component.text("Select a casting mold"))
                        .build();
            }

            final ItemInstance instance = itemFactory.create(castingMold);
            return ItemView.of(instance.getView().get()).toBuilder()
                    .action(ClickActions.ALL, Component.text("Select a casting mold"))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            // Open casting mold picker GUI
            picker.show(player);
        }
    }

    private class WarningIcon extends AutoUpdateItem {

        private WarningIcon() {
            super(1, null); // we can use null here because we override getItemProvider
        }

        @Override
        public ItemProvider getItemProvider() {
            // If we have items and there isnt a matching recipe
            if (!contentInventory.isEmpty() && data.getProcessingEngine().getCurrentSmeltingRecipe() == null) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/mini_furnace_disabled"))
                        .displayName(Component.text("No smelting recipe found!", NamedTextColor.RED))
                        .lore(Component.text("No matching smelting recipe was found for the given items.", NamedTextColor.GRAY))
                        .build();
            }

            // If we're not burning and we have a recipe'
            if (!data.isBurning() && data.getProcessingEngine().getCurrentSmeltingRecipe() != null) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/mini_furnace_disabled"))
                        .displayName(Component.text("Not burning!", NamedTextColor.RED))
                        .lore(Component.text("The smelter is not currently burning. Add fuel to start the smelting process.", NamedTextColor.GRAY))
                        .build();
            }

            // If we have an alloy and there isnt a matching casting recipe
            if (data.getLiquidManager().hasLiquid() && data.getProcessingEngine().getCurrentCastingRecipe() == null) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/mini_furnace_disabled"))
                        .displayName(Component.text("No casting recipe found!", NamedTextColor.RED))
                        .lore(Component.text("No matching casting recipe was found for the given items.", NamedTextColor.GRAY))
                        .build();
            }

            // If what we're smelting isn't compatible with what's in already
            if (!data.getLiquidManager().isCompatibleWith(data.getProcessingEngine().getSmeltingAlloy())) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/mini_furnace_disabled"))
                        .displayName(Component.text("Incompatible alloy!", NamedTextColor.RED))
                        .lore(Component.text("The smelter already contains a different type of alloy.", NamedTextColor.GRAY))
                        .build();
            }

            // If what we're smelting can't fit into the liquid storage
            if (data.getProcessingEngine().getSmeltingAlloy() != null
                    && !data.getLiquidManager().hasCapacityFor(data.getProcessingEngine().getSmeltingAlloy().getMillibuckets())) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/mini_furnace_disabled"))
                        .displayName(Component.text("Not enough liquid capacity!", NamedTextColor.RED))
                        .lore(Component.text("The smelter does not have enough capacity to store the resulting liquid.", NamedTextColor.GRAY))
                        .build();
            }

            // If the liquid storage is full
            if (data.getLiquidManager().getRemainingLiquidCapacity() <= 0) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/mini_furnace_disabled"))
                        .displayName(Component.text("Liquid storage full!", NamedTextColor.RED))
                        .lore(Component.text("The smelter's liquid storage is full. Clear some space to continue smelting.", NamedTextColor.GRAY))
                        .build();
            }

            // If we're not at the necessary temperature and we have a recipe
            if (data.getProcessingEngine().getCurrentSmeltingRecipe() != null
                    && data.getTemperature() < data.getProcessingEngine().getCurrentSmeltingRecipe().getMinimumTemperature()) {
                float correctTemp = data.getProcessingEngine().getCurrentSmeltingRecipe().getMinimumTemperature();
                final TextColor color = new ProgressColor(correctTemp / 1_000).inverted().getTextColor();
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/mini_furnace_disabled"))
                        .displayName(Component.text("Insufficient temperature!", NamedTextColor.RED))
                        .lore(Component.text("The smelter's temperature is too low. Minimum required: ", TextColor.color(214, 214, 214))
                                .appendSpace()
                                .append(Component.text((int) correctTemp + " °C", color)))
                        .build();
            }

            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/icon/regular/mini_furnace"))
                    .hideTooltip(true)
                    .build();
        }
    }
}