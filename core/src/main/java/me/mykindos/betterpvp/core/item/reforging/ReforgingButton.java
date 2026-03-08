package me.mykindos.betterpvp.core.item.reforging;

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
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatAugmentation;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatAugmentationComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatType;
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBias;
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBiasRegistry;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Dynamic compound button for reforging GUI that validates requirements
 * and executes reforging when clicked.
 */
@RequiredArgsConstructor
public class ReforgingButton extends ControlItem<Gui> {

    private final ItemFactory itemFactory;
    private final VirtualInventory goldInventory;
    private final VirtualInventory powderInventory;
    private final VirtualInventory itemInventory;
    private final PurityReforgeBiasRegistry biasRegistry;
    private final boolean visible;


    @Override
    public ItemProvider getItemProvider(Gui gui) {
        List<Component> errors = validateReforge();

        if (errors.isEmpty()) {
            // Success state - show delta preview
            ItemView.ItemViewBuilder builder = ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(visible ? Key.key("betterpvp", "menu/gui/reforge/button_success") : Material.AIR.getKey())
                    .displayName(Component.text("GO!", NamedTextColor.GREEN, TextDecoration.BOLD));

            // Add stat preview lore
            List<Component> deltaPreview = generateDeltaPreview();
            if (!deltaPreview.isEmpty()) {
                builder.lore(Component.empty());
                builder.lore(Component.text("Stats:", NamedTextColor.YELLOW, TextDecoration.BOLD));
                deltaPreview.forEach(builder::lore);
                builder.lore(Component.empty());
                builder.lore(Component.empty()
                        .append(Component.text("WARNING:", NamedTextColor.RED, TextDecoration.BOLD))
                        .appendSpace()
                        .append(Component.text("All stats will be randomized!", NamedTextColor.RED, TextDecoration.ITALIC)));
            }

            ItemRarity rarity = itemFactory.fromItemStack(Objects.requireNonNull(itemInventory.getItem(0))).orElseThrow().getRarity();
            builder.lore(Component.empty()
                    .append(Component.text("Cost:", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .appendSpace()
                    .append(Component.text(getReforgingCost(rarity) + " gold", NamedTextColor.YELLOW)));
            return builder.build();
        } else {
            // Error state
            ItemView.ItemViewBuilder builder = ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(visible ? Key.key("betterpvp", "menu/gui/reforge/button_error") : Material.AIR.getKey());

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

    private List<Component> validateReforge() {
        List<Component> errors = new ArrayList<>();

        // Check if item slot has an item
        ItemStack itemStack = itemInventory.getItem(0);
        if (itemStack == null) {
            errors.add(Component.text("No item to reforge", NamedTextColor.RED));
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
            errors.add(Component.text("Cannot reforge multiple items", NamedTextColor.RED));
        }

        // Check for StatContainerComponent
        Optional<StatContainerComponent> purityOpt = itemInstance.getComponent(StatContainerComponent.class);
        if (purityOpt.isEmpty()) {
            errors.add(Component.text("Item cannot hold stats", NamedTextColor.RED));
            return errors;
        }

        StatContainerComponent statContainer = purityOpt.get();

        // Check stat powder (optional)
        ItemStack powderStack = powderInventory.getItem(0);
        if (powderStack != null) {
            // Check compatibility for at least one stat
            StatAugmentationComponent reforgeComponent = itemFactory.fromItemStack(powderStack).orElseThrow()
                    .getComponent(StatAugmentationComponent.class)
                    .orElseThrow();

            final List<ItemStat<?>> stats = statContainer.getStats();
            boolean compatible = false;
            for (StatAugmentation reforge : reforgeComponent.getAugmentations()) {
                final StatType<?> type = reforge.getType();

                compatible = compatible || stats.stream().anyMatch(stat -> stat.getType().equals(type));
            }

            if (!compatible) {
                errors.add(Component.text("No compatible stats to reforge", NamedTextColor.RED));
            }
        }

        // Check gold
        ItemStack goldStack = goldInventory.getItem(0);
        long cost = getReforgingCost(itemInstance.getRarity());
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

    private long getReforgingCost(ItemRarity rarity) {
        Core core = JavaPlugin.getPlugin(Core.class);
        String configPath = "items.reforging.costs." + rarity.name();

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

    /**
     * Generates a preview of stat changes that will occur from reforging.
     * Always shows all stats on the item.
     * If a stat is being augmented, shows "old → new" format.
     * If a stat is not being augmented, shows "min-max" format.
     * Also shows which augmentations will be voided due to missing stats.
     *
     * @return List of components describing each stat change
     */
    private List<Component> generateDeltaPreview() {
        List<Component> preview = new ArrayList<>();

        ItemStack itemStack = itemInventory.getItem(0);
        ItemStack powderStack = powderInventory.getItem(0);

        if (itemStack == null) {
            return preview;
        }

        Optional<ItemInstance> itemInstanceOpt = itemFactory.fromItemStack(itemStack);
        if (itemInstanceOpt.isEmpty()) {
            return preview;
        }

        ItemInstance itemInstance = itemInstanceOpt.get();
        Optional<StatContainerComponent> statContainerOpt = itemInstance.getComponent(StatContainerComponent.class);
        if (statContainerOpt.isEmpty()) {
            return preview;
        }

        StatContainerComponent statContainer = statContainerOpt.get();
        StatAugmentationComponent reforgeComponent = null;

        // Get augmentation component if powder is present
        if (powderStack != null) {
            Optional<ItemInstance> powderInstanceOpt = itemFactory.fromItemStack(powderStack);
            if (powderInstanceOpt.isPresent()) {
                reforgeComponent = powderInstanceOpt.get()
                        .getComponent(StatAugmentationComponent.class)
                        .orElse(null);
            }
        }

        List<Component> voidedReforges = new ArrayList<>();

        // Show all stats
        for (ItemStat<?> stat : statContainer.getStats()) {
            StatType<?> statType = stat.getType();

            // Check if this stat is being augmented
            StatAugmentation matchingAugmentation = null;
            if (reforgeComponent != null) {
                for (StatAugmentation augmentation : reforgeComponent.getAugmentations()) {
                    if (augmentation.getType().equals(statType)) {
                        matchingAugmentation = augmentation;
                        break;
                    }
                }
            }

            if (matchingAugmentation != null) {
                // This stat is being augmented
                ItemStat<?> newStat = matchingAugmentation.apply(stat);

                Component line = Component.empty()
                        .append(Component.text("  " + statType.getShortName() + ": ", NamedTextColor.GRAY))
                        .append(Component.text(formatStatValue(stat), NamedTextColor.WHITE))
                        .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(formatStatValue(newStat), NamedTextColor.GREEN));

                preview.add(line);
            } else {
                // This stat is not being augmented, just show the range
                Component line = Component.empty()
                        .append(Component.text("  " + statType.getShortName() + ": ", NamedTextColor.GRAY))
                        .append(Component.text(formatStatValue(stat), NamedTextColor.WHITE));

                preview.add(line);
            }
        }

        // Show voided augmentations (augmentations for stats that don't exist on the item)
        if (reforgeComponent != null) {
            for (StatAugmentation augmentation : reforgeComponent.getAugmentations()) {
                Optional<? extends ItemStat<?>> statOpt = statContainer.getStat(augmentation.getType());
                if (statOpt.isEmpty()) {
                    Component voidedLine = Component.empty()
                            .append(Component.text("  " + augmentation.getType().getShortName(), NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
                            .append(Component.text(" (no stat)", NamedTextColor.DARK_GRAY));
                    voidedReforges.add(voidedLine);
                }
            }
        }

        // Add voided augmentations section if any
        if (!voidedReforges.isEmpty()) {
            if (!preview.isEmpty()) {
                preview.add(Component.empty()); // Spacing
            }
            preview.add(Component.text("Voided Augmentations:", NamedTextColor.RED, TextDecoration.BOLD));
            preview.addAll(voidedReforges);
        }

        return preview;
    }

    /**
     * Formats a stat value showing its current range (min-max).
     *
     * @param stat The stat to format
     * @return Formatted string like "5.0-7.0"
     */
    private <T> String formatStatValue(ItemStat<T> stat) {
        return (stat.getType().stringValue(stat.getRangeMin()) + "‑" + stat.getType().stringValue(stat.getRangeMax()))
                .replace("+", "");
    }

    /**
     * Randomizes a stat's value between its current range min and max,
     * applying purity-based bias if the item has purity.
     *
     * @param stat   The stat to randomize
     * @param purityComponent The purity of the item (nullable)
     * @return A new stat with a biased random value
     */
    private <T> ItemStat<T> randomizeStat(ItemStat<T> stat, @Nullable PurityComponent purityComponent) {
        ItemPurity purity = ItemPurity.PITIFUL;
        if (purityComponent != null && purityComponent.isAttuned()) {
            purity = purityComponent.getPurity();
        }

        // Apply purity-based bias using beta distribution
        PurityReforgeBias bias = biasRegistry.getBias(purity);
        double biasRatio = bias.generateBiasedRatio();
        T randomValue = stat.getType().randomBetweenBiased(
                stat.getRangeMin(),
                stat.getRangeMax(),
                biasRatio
        );

        return stat.withValue(randomValue);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!clickType.isLeftClick()) {
            return;
        }

        // Validate before executing
        List<Component> errors = validateReforge();
        if (!errors.isEmpty()) {
            new SoundEffect(Sound.ITEM_BRUSH_BRUSHING_GENERIC, 1f, 1.5f).play(player);
            new SoundEffect(Sound.UI_HUD_BUBBLE_POP, 0f, 1f).play(player);
            return; // Don't execute if there are errors
        }

        // Execute reforge
        executeReforge(player, event);
    }

    private void executeReforge(Player player, InventoryClickEvent event) {
        // Get all items
        ItemStack itemStack = Objects.requireNonNull(itemInventory.getItem(0));
        ItemStack goldStack = Objects.requireNonNull(goldInventory.getItem(0));
        ItemStack reforgeStack = powderInventory.getItem(0); // Powder is optional

        // Convert to ItemInstances
        ItemInstance itemInstance = itemFactory.fromItemStack(itemStack).orElseThrow();
        ItemInstance goldInstance = itemFactory.fromItemStack(goldStack).orElseThrow();

        // Get stats and cost
        StatContainerComponent statContainer = itemInstance.getComponent(StatContainerComponent.class).orElseThrow();
        long cost = getReforgingCost(itemInstance.getRarity());

        // Get augmentation component if powder is present
        StatAugmentationComponent reforgeComponent = null;
        if (reforgeStack != null) {
            ItemInstance powderInstance = itemFactory.fromItemStack(reforgeStack).orElseThrow();
            reforgeComponent = powderInstance.getComponent(StatAugmentationComponent.class).orElse(null);
        }

        // Call the event
        PlayerReforgeItemEvent reforgeItemEvent = new PlayerReforgeItemEvent(player, itemInstance, reforgeComponent);
        if (UtilServer.callEvent(reforgeItemEvent).isCancelled()) {
            return;
        }

        // Apply augmentations if powder is present
        if (reforgeComponent != null) {
            for (StatAugmentation statAugmentation : reforgeComponent.getAugmentations()) {
                final Optional<? extends ItemStat<?>> statOpt = statContainer.getStat(statAugmentation.getType());
                if (statOpt.isEmpty()) {
                    continue;
                }

                final ItemStat<?> stat = statOpt.get();
                ItemStat<?> reforgedStat = statAugmentation.apply(stat);

                // Update modifier stats, not base stats (modifier stats are serialized)
                statContainer.withModifierStat(reforgedStat);
            }
        }

        // Extract purity once, use for all stats
        final PurityComponent purity = itemInstance.getComponent(PurityComponent.class).orElse(null);

        // After augmentation (or without it), randomize ALL stats with purity bias
        for (ItemStat<?> stat : statContainer.getStats()) {
            ItemStat<?> randomizedStat = randomizeStat(stat, purity);
            statContainer.withModifierStat(randomizedStat);
        }

        ItemInstance reforgedInstance = itemInstance.withComponent(statContainer);

        // Consume gold
        ItemInstance remainingGold = CurrencyUtils.subtract(goldInstance, cost);

        // Consume powder if present (reduce by 1)
        ItemStack remainingPowder = null;
        if (reforgeStack != null) {
            remainingPowder = reforgeStack.clone();
            remainingPowder.subtract();
            if (remainingPowder.getAmount() <= 0 || remainingPowder.getType().isAir()) {
                remainingPowder = null;
            }
        }

        // Update inventories
        PlayerUpdateReason reason = new PlayerUpdateReason(player, event);
        itemInventory.setItem(reason, 0, reforgedInstance.createItemStack());

        if (remainingGold.getItemStack().getAmount() > 0) {
            goldInventory.setItem(reason, 0, remainingGold.createItemStack());
        } else {
            goldInventory.setItem(reason, 0, null);
        }

        powderInventory.setItem(reason, 0, remainingPowder);

        // Update button state
        notifyWindows();
    }
}
