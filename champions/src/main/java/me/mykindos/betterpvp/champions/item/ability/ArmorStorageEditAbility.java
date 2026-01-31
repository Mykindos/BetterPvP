package me.mykindos.betterpvp.champions.item.ability;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.component.storage.ArmorStorageComponent;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.PlayerUpdateReason;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SuppliedItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.menu.button.PlaceholderInventorySlot;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class ArmorStorageEditAbility extends AbstractInteraction {

    public ArmorStorageEditAbility() {
        super("Replace Armor", "Add or remove equipment stored on this item.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!actor.isPlayer() || itemInstance == null) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final ArmorStorageComponent component = itemInstance.getComponent(ArmorStorageComponent.class)
                .orElseThrow(() -> new IllegalStateException("Item does not have armor storage component"));

        final Player player = (Player) actor.getEntity();
        new Editor(component, itemInstance).show(player);
        return InteractionResult.Success.ADVANCE;
    }

    private static class Editor extends AbstractGui implements Windowed {

        private final SuppliedItem observedItem;

        private Editor(ArmorStorageComponent component, ItemInstance item) {
            super(9, 3);
            // Armor storage
            VirtualInventory helmet = createSlot(EquipmentSlot.HEAD, component, item);
            VirtualInventory chestplate = createSlot(EquipmentSlot.CHEST, component, item);
            VirtualInventory leggings = createSlot(EquipmentSlot.LEGS, component, item);
            VirtualInventory boots = createSlot(EquipmentSlot.FEET, component, item);

            this.observedItem = new SuppliedItem(item::getView, null);

            // Structure
            applyStructure(new Structure(
                    "00000000I",
                    "0HCLB00P0",
                    "000000000")
                    .addIngredient('0', Menu.INVISIBLE_BACKGROUND_ITEM)
                    .addIngredient('I', InfoTabButton.builder()
                            .description(Component.text("Place your equipment to store them inside this item."))
                            .build())
                    .addIngredient('H', new PlaceholderInventorySlot(helmet, getPlaceholder("helmet")))
                    .addIngredient('C', new PlaceholderInventorySlot(chestplate, getPlaceholder("chestplate")))
                    .addIngredient('L', new PlaceholderInventorySlot(leggings, getPlaceholder("leggings")))
                    .addIngredient('B', new PlaceholderInventorySlot(boots, getPlaceholder("boots")))
                    .addIngredient('P', observedItem));
        }

        @Override
        public @NotNull Component getTitle() {
            return Component.text("<shift:-8><glyph:menu_armor_storage>").font(NEXO);
        }

        private ItemProvider getPlaceholder(String itemModel) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/sprite/slot/" + itemModel))
                    .hideTooltip(true)
                    .build();
        }

        private VirtualInventory createSlot(EquipmentSlot equipmentSlot, ArmorStorageComponent component, ItemInstance itemInstance) {
            final VirtualInventory virtualInventory = new VirtualInventory(UUID.randomUUID(), new ItemStack[] {
                    component.getItem(equipmentSlot)
            });

            // Disallow items that aren't part of this slot
            virtualInventory.setPreUpdateHandler(event -> {
                final ItemStack newItem = event.getNewItem();
                // Item isn't armor
                if (newItem != null && newItem.getType().getEquipmentSlot() != equipmentSlot) {
                    event.setCancelled(true);
                    return;
                }

                // Item isn't allowed
                final ItemFactory itemFactory = JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(ItemFactory.class);
                if (newItem != null && !component.canStore(itemFactory.fromItemStack(newItem).orElseThrow())) {
                    event.setCancelled(true);
                }
            });

            // Update the component
            virtualInventory.setPostUpdateHandler(event -> {
                component.setItem(equipmentSlot, event.getNewItem());
                itemInstance.serializeAllComponentsToItemStack(); // update the item
                observedItem.notifyWindows(); // update the menu with the item

                if (event.getUpdateReason() instanceof PlayerUpdateReason reason) {
                    final float pitch = event.getNewItem() == null ? 0.8f : 1.2f;
                    new SoundEffect(Sound.ENTITY_HORSE_ARMOR, pitch, 0.4f).play(reason.getPlayer().getLocation());
                }
            });

            return virtualInventory;
        }
    }
}
