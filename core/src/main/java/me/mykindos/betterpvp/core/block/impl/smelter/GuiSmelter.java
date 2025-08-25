package me.mykindos.betterpvp.core.block.impl.smelter;

import com.ticxo.modelengine.api.ModelEngineAPI;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.PlayerUpdateReason;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.builder.ItemBuilder;
import me.mykindos.betterpvp.core.inventory.item.impl.AutoUpdateItem;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.fuel.FuelComponent;
import me.mykindos.betterpvp.core.menu.Windowed;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@SuppressWarnings("ALL")
@CustomLog
public class GuiSmelter extends AbstractGui implements Windowed {

    private final SmelterData data;
    private final ItemFactory itemFactory;
    private final int maxFuelSegments = 4;
    private final VirtualInventory contentInventory;
    private final VirtualInventory fuelInventory;
    private final VirtualInventory resultInventory;
    private final GuiCastingMoldPicker picker;

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
                "0000000A0",
                "0YYYYY0BT",
                "0YYYYY0CT",
                "000X000DT",
                "000WH00E0",
                "000Z000F0")
                // Fuel meter
                .addIngredient('A', new FuelMeter(4))
                .addIngredient('B', new FuelMeter(3))
                .addIngredient('C', new FuelMeter(2))
                .addIngredient('D', new FuelMeter(1))
                .addIngredient('E', new FuelMeter())
                // Fuel inventory
                .addIngredient('F', fuelInventory)
                // Casting mold slot
                .addIngredient('H', new CastingMoldPicker())
                // Temperature meter
                .addIngredient('T', new TemperatureMeter())
                // Content inventory
                .addIngredient('Y', contentInventory)
                // Results (liquid and casting molds)
                .addIngredient('X', new AlloyStorage(false))
                .addIngredient('W', new AlloyStorage(true))
                .addIngredient('Z', resultInventory)
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
            return;
        }

        // Check if we have a casting mold selected
        if (data.getProcessingEngine().getCastingMold() == null) {
            // No casting mold selected, show barrier
            ItemStack barrier = ItemStack.of(Material.BARRIER);
            barrier.editMeta(meta -> meta.displayName(Component.text("No Casting Mold Selected", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)));
            resultInventory.setItemSilently(0, barrier);
            return;
        }

        // Check if we have stored liquid and if there's a valid recipe
        if (data.getLiquidManager().getStoredLiquid() == null || data.getProcessingEngine().getCurrentRecipe() == null) {
            // No valid recipe available, show barrier
            ItemStack barrier = ItemStack.of(Material.BARRIER);
            barrier.editMeta(meta -> meta.displayName(Component.text("No Recipe Found", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)));
            resultInventory.setItemSilently(0, barrier);
            return;
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
            final ItemStack stack = item.createItemStack();
            inventory.setItemSilently(i, stack);
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_smelter>").font(NEXO);
    }

    private class FuelMeter extends AutoUpdateItem {

        private final int segment;

        private FuelMeter(int segment) {
            super(1, null); // we can use null here because we override getItemProvider
            this.segment = segment;
        }

        private FuelMeter() {
            this(-1);
        }

        private Component getDisplayName(float percentage) {
            final ProgressColor progressColor = new ProgressColor(percentage);
            return Component.text("Fuel:", TextColor.color(214, 214, 214))
                    .appendSpace()
                    .append(Component.text((int) (percentage * 100) + "%", progressColor.getTextColor()));
        }

        @Override
        public ItemProvider getItemProvider() {
            final float percentage = Math.max(0, Math.min(1, data.getBurnTime() / (float) data.getMaxBurnTime()));
            final float totalFill = percentage * maxFuelSegments;

            final ItemStack itemStack = ItemStack.of(Material.PAPER);
            final ItemMeta meta = itemStack.getItemMeta();
            meta.displayName(getDisplayName(percentage).decoration(TextDecoration.ITALIC, false));
            itemStack.setItemMeta(meta);

            // Calculate how full this specific segment is
            if (segment != -1) {
                float segmentFill = totalFill - (segment - 1); // 1.0 = full, 0.0 = empty
                if (segmentFill <= 0) {
                    // Empty → invisible
                    itemStack.setData(DataComponentTypes.ITEM_MODEL, Resources.ItemModel.INVISIBLE);
                    return new ItemBuilder(itemStack);
                }

                // Clamp to [0.0, 1.0] range
                segmentFill = Math.min(1.0f, segmentFill);

                // Determine the stage (0 = full, 3 = 25%)
                int fillStage = 3 - Math.min(3, (int) (segmentFill * 4.0f));
                int modelIndex = (4 - segment) * 4 + fillStage;

                itemStack.setData(DataComponentTypes.ITEM_MODEL, Key.key("betterpvp", "menu/smelter/fuel_bar_generic"));
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                        CustomModelData.customModelData().addString(String.valueOf(modelIndex)).build());
            } else {
                itemStack.setData(DataComponentTypes.ITEM_MODEL, Resources.ItemModel.INVISIBLE);
            }

            return new ItemBuilder(itemStack);
        }
    }

    private class TemperatureMeter extends AutoUpdateItem {

        private TemperatureMeter() {
            super(1, null);
        }

        @Override
        public ItemProvider getItemProvider() {
            final float temperature = data.getTemperature();
            final TextColor color = new ProgressColor(temperature / 1_000).inverted().getTextColor();
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Resources.ItemModel.INVISIBLE)
                    .displayName(Component.text("Temperature: ", TextColor.color(214, 214, 214))
                            .appendSpace()
                            .append(Component.text((int) temperature + " °C", color)))
                    .build();
        }
    }

    private class AlloyStorage extends AutoUpdateItem {

        private final boolean invisible;

        private AlloyStorage(boolean invisible) {
            super(1, null); // we can use null here because we override getItemProvider
            this.invisible = invisible;
        }

        @Override
        public ItemProvider getItemProvider() {
            final LiquidAlloy storedLiquid = data.getLiquidManager().getStoredLiquid();
            final int millibuckets = storedLiquid == null ? 0 : storedLiquid.getMillibuckets();
            final int maxMillibuckets = data.getMaxLiquidCapacity();
            final Key model = storedLiquid == null
                    ? Resources.ItemModel.INVISIBLE
                    : Key.key("betterpvp", "menu/smelter/output");
            final TextColor storedColor = new ProgressColor(millibuckets / (float) maxMillibuckets).inverted().getTextColor();
            final TextComponent name = storedLiquid == null
                    ? Component.text("Empty", TextColor.color(255, 86, 74))
                    : Component.text(storedLiquid.getName(), TextColor.color(storedLiquid.getAlloyType().getColor().asRGB()), TextDecoration.BOLD);

            final ItemStack item = ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(invisible ? Resources.ItemModel.INVISIBLE : model)
                    .displayName(name)
                    .lore(Component.text("Stored: ", TextColor.color(214, 214, 214))
                            .appendSpace()
                            .append(Component.text(millibuckets + " mB", storedColor)))
                    .lore(Component.text("Max Capacity: ", TextColor.color(214, 214, 214))
                            .appendSpace()
                            .append(Component.text(maxMillibuckets + " mB", TextColor.color(255, 0, 0))))
                    .build().get();

            if (storedLiquid != null) {
                final CustomModelData modelData = CustomModelData.customModelData()
                        .addColor(storedLiquid.getAlloyType().getColor())
                        .build();
                item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, modelData);
            }

            return new ItemBuilder(item);
        }
    }

    private class CastingMoldPicker extends ControlItem<GuiSmelter> {

        @Override
        public ItemProvider getItemProvider(GuiSmelter gui) {
            final CastingMold castingMold = data.getProcessingEngine().getCastingMold();
            if (castingMold == null) {
                return ItemView.builder()
                        .material(Material.BARRIER)
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
}