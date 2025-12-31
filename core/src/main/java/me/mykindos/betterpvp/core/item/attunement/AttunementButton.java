package me.mykindos.betterpvp.core.item.attunement;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.PlayerUpdateReason;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.currency.CurrencyUtils;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Dynamic compound button for attunement GUI that validates requirements
 * and executes attunement when clicked.
 */
@RequiredArgsConstructor
public class AttunementButton extends ControlItem<Gui> {

    private final ItemFactory itemFactory;
    private final VirtualInventory goldInventory;
    private final VirtualInventory stoneInventory;
    private final VirtualInventory itemInventory;
    private final boolean visible;

    @Override
    public ItemProvider getItemProvider(Gui gui) {
        List<Component> errors = validateAttunement();

        if (errors.isEmpty()) {
            // Success state
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(visible ? Key.key("betterpvp", "menu/gui/attunement/button_success") : Material.AIR.getKey())
                    .displayName(Component.text("GO!", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .build();
        } else {
            // Error state
            ItemView.ItemViewBuilder builder = ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(visible ? Key.key("betterpvp", "menu/gui/attunement/button_error") : Material.AIR.getKey());

            // First error becomes display name
            builder.displayName(Component.empty()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("✘", NamedTextColor.DARK_RED))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(errors.getFirst()));

            // Additional errors become lore
            for (int i = 1; i < errors.size(); i++) {
                builder.lore(Component.empty()
                        .append(Component.text("[", NamedTextColor.GRAY))
                        .append(Component.text("✘", NamedTextColor.DARK_RED))
                        .append(Component.text("]", NamedTextColor.GRAY))
                        .appendSpace()
                        .append(errors.get(i)));
            }

            return builder.build();
        }
    }

    private List<Component> validateAttunement() {
        List<Component> errors = new ArrayList<>();

        // Check if item slot has an item
        ItemStack itemStack = itemInventory.getItem(0);
        if (itemStack == null) {
            errors.add(Component.text("No item to attune", NamedTextColor.RED));
            return errors; // Can't validate further
        }

        // Get ItemInstance
        Optional<ItemInstance> itemInstanceOpt = itemFactory.fromItemStack(itemStack);
        if (itemInstanceOpt.isEmpty()) {
            errors.add(Component.text("Invalid item", NamedTextColor.RED));
            return errors;
        }

        ItemInstance itemInstance = itemInstanceOpt.get();
        if (itemInstance.getItemStack().getAmount() > 1) {
            errors.add(Component.text("Cannot attune multiple items", NamedTextColor.RED));
        }

        // Check for PurityComponent
        Optional<PurityComponent> purityOpt = itemInstance.getComponent(PurityComponent.class);
        if (purityOpt.isEmpty()) {
            errors.add(Component.text("Item has no purity", NamedTextColor.RED));
            return errors;
        }

        PurityComponent purity = purityOpt.get();

        // Check if already attuned
        if (purity.isAttuned()) {
            errors.add(Component.text("Already attuned", NamedTextColor.RED));
        }

        // Check attunement stone
        ItemStack stoneStack = stoneInventory.getItem(0);
        if (stoneStack == null) {
            errors.add(Component.text("Need attunement stone", NamedTextColor.RED));
        }

        // Check gold
        ItemStack goldStack = goldInventory.getItem(0);
        long cost = getAttunementCost(itemInstance.getRarity());
        if (goldStack == null) {
            errors.add(Component.text("Need " + cost + " gold", NamedTextColor.RED));
        } else {
            // Validate gold amount
            Optional<ItemInstance> goldInstanceOpt = itemFactory.fromItemStack(goldStack);
            if (goldInstanceOpt.isPresent()) {
                ItemInstance goldInstance = goldInstanceOpt.get();

                if (!CurrencyUtils.canSubtract(goldInstance, cost)) {
                    errors.add(Component.text("Need " + cost + " gold", NamedTextColor.RED));
                }
            }
        }

        return errors;
    }

    private long getAttunementCost(ItemRarity rarity) {
        Core core = JavaPlugin.getPlugin(Core.class);
        String configPath = "items.attunement.costs." + rarity.name();

        // Default costs by rarity
        long defaultCost = switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 5;
            case LEGENDARY -> 10;
            case MYTHICAL -> 15;
        };

        return core.getConfig().getOrSaveInt(configPath, (int) defaultCost);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!clickType.isLeftClick()) {
            return;
        }

        // Validate before executing
        List<Component> errors = validateAttunement();
        if (!errors.isEmpty()) {
            new SoundEffect(Sound.ITEM_SPYGLASS_USE, 0, 1.5f).play(player);
            new SoundEffect(Sound.UI_HUD_BUBBLE_POP, 0f, 1f).play(player);
            return; // Don't execute if there are errors
        }

        // Execute attunement
        executeAttunement(player, event);
    }

    private void executeAttunement(Player player, InventoryClickEvent event) {
        // Get all items
        ItemStack itemStack = Objects.requireNonNull(itemInventory.getItem(0));
        ItemStack goldStack = Objects.requireNonNull(goldInventory.getItem(0));
        ItemStack stoneStack = Objects.requireNonNull(stoneInventory.getItem(0));

        // Convert to ItemInstances
        ItemInstance itemInstance = itemFactory.fromItemStack(itemStack).orElseThrow();
        ItemInstance goldInstance = itemFactory.fromItemStack(goldStack).orElseThrow();

        // Get purity and cost
        PurityComponent purity = itemInstance.getComponent(PurityComponent.class).orElseThrow();
        long cost = getAttunementCost(itemInstance.getRarity());

        // Call the event
        PlayerAttuneItemEvent attuneEvent = new PlayerAttuneItemEvent(player, itemInstance, purity);
        if (UtilServer.callEvent(attuneEvent).isCancelled()) {
            return;
        }

        // Attune the item
        PurityComponent attuned = purity.withAttuned(true);
        ItemInstance attunedInstance = itemInstance.withComponent(attuned);

        // Consume gold
        ItemInstance remainingGold = CurrencyUtils.subtract(goldInstance, cost);

        // Consume attunement stone (reduce by 1)
        ItemStack remainingStone = stoneStack.clone();
        remainingStone.subtract();
        if (remainingStone.getAmount() <= 0 || remainingStone.getType().isAir()) {
            remainingStone = null;
        }

        // Update inventories
        PlayerUpdateReason reason = new PlayerUpdateReason(player, event);
        itemInventory.setItem(reason, 0, attunedInstance.createItemStack());

        if (remainingGold.getItemStack().getAmount() > 0) {
            goldInventory.setItem(reason, 0, remainingGold.createItemStack());
        } else {
            goldInventory.setItem(reason, 0, null);
        }

        stoneInventory.setItem(reason, 0, remainingStone);

        // Update button state
        notifyWindows();
    }
}
