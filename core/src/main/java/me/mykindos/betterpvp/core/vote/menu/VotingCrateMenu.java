package me.mykindos.betterpvp.core.vote.menu;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.ItemWrapper;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.economy.CoinLoot;
import me.mykindos.betterpvp.core.loot.item.ItemLoot;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class VotingCrateMenu extends AbstractGui {

    private static final int ROWS = 3;
    private static final int COLUMNS = 9;
    private static final int MIDDLE_ROW_START = 9;

    private record RollItem(ItemStack displayItem, ItemStack rewardItem) {}

    private final LootTable lootTable;
    private final List<RollItem> rollItems = new ArrayList<>();
    private final Random random = new Random();
    private final ItemRegistry itemRegistry;

    public VotingCrateMenu(ItemRegistry itemRegistry, LootTableRegistry lootTableRegistry) {
        super(COLUMNS, ROWS);
        this.itemRegistry = itemRegistry;
        this.lootTable = lootTableRegistry.loadLootTable("voting_crate");

        // Fill borders
        ItemProvider border = new ItemWrapper(ItemView.builder().material(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.text(" ")).build().toItemStack());
        for (int i = 0; i < 9; i++) {
            setSlotElement(i, new SlotElement.ItemSlotElement(new SimpleItem(border)));
            setSlotElement(i + 18, new SlotElement.ItemSlotElement(new SimpleItem(border)));
        }

        // Target indicators
        ItemProvider target = new ItemWrapper(ItemView.builder().material(Material.HOPPER).displayName(Component.text("Winning Slot", NamedTextColor.YELLOW)).build().toItemStack());
        setSlotElement(4, new SlotElement.ItemSlotElement(new SimpleItem(target)));
        setSlotElement(22, new SlotElement.ItemSlotElement(new SimpleItem(target)));

        // Pre-fill roll items
        if (!lootTable.getWeightedLoot().isEmpty()) {
            for (int i = 0; i < 9; i++) {
                rollItems.add(createRollItem(getRandomWeightedLoot()));
            }
        }
    }

    private RollItem createRollItem(Loot<?, ?> loot) {
        ItemStack displayItem = loot.getIcon().get();
        ItemStack rewardItem = displayItem.clone();
        if (loot instanceof ItemLoot<?> itemLoot) {
            final BaseItem baseItem = itemRegistry.getItem(itemLoot.getItemKey());
            if (baseItem != null) {
                rewardItem = baseItem.getModel();
            }
            int amount = itemLoot.getMinAmount();
            if (itemLoot.getMaxAmount() > itemLoot.getMinAmount()) {
                amount = random.nextInt(itemLoot.getMaxAmount() - itemLoot.getMinAmount() + 1) + itemLoot.getMinAmount();
            }
            displayItem.setAmount(amount);
            rewardItem.setAmount(amount);
        } else if (loot instanceof CoinLoot<?> coinLoot) {
            int amount = coinLoot.getMinAmount();
            if (coinLoot.getMaxAmount() > coinLoot.getMinAmount()) {
                amount = random.nextInt(coinLoot.getMaxAmount() - coinLoot.getMinAmount() + 1) + coinLoot.getMinAmount();
            }
            ItemStack coinItem = coinLoot.getCoinType().generateItem(amount);
            displayItem = coinItem;
            rewardItem = coinItem.clone();
        }
        return new RollItem(displayItem, rewardItem);
    }

    private Loot<?, ?> getRandomWeightedLoot() {
        Map<Loot<?, ?>, Float> chances = lootTable.getChances();
        float totalWeight = 0;
        for (float chance : chances.values()) {
            totalWeight += chance;
        }

        float r = random.nextFloat() * totalWeight;
        float cumulative = 0;
        for (Map.Entry<Loot<?, ?>, Float> entry : chances.entrySet()) {
            cumulative += entry.getValue();
            if (r <= cumulative) {
                return entry.getKey();
            }
        }

        return chances.keySet().iterator().next();
    }

    public void show(Player player) {
        if (lootTable.getWeightedLoot().isEmpty()) {
            UtilMessage.simpleMessage(player, "Voting", "<red>The voting crate is currently empty. Please contact an administrator.");
            return;
        }

        Window.single()
                .setGui(this)
                .setTitle(Component.text("Voting Crate", NamedTextColor.DARK_AQUA))
                .open(player);

        startRolling(player);
    }

    private void startRolling(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            int delay = 1;
            int count = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (count > 20) delay = 2;
                if (count > 28) delay = 3;
                if (count > 33) delay = 5;
                if (count > 38) delay = 10;

                if (ticks % delay == 0) {
                    roll();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
                    count++;
                }

                ticks++;

                if (count > 43) {
                    RollItem winningRoll = rollItems.get(4);
                    ItemStack winner = winningRoll.rewardItem(); // Winning slot is at index 4 (middle of 9)
                    if (winner != null) {
                        Component name;
                        if (winner.getItemMeta().hasDisplayName()) {
                            name = Objects.requireNonNull(winner.getItemMeta().displayName());
                        } else {
                            name = Objects.requireNonNullElse(winner.getData(DataComponentTypes.ITEM_NAME),
                                    Component.translatable(winner.getType().translationKey()));
                        }
                        Component winText = Component.text("You won: ", NamedTextColor.YELLOW)
                                .append(name.hoverEvent(winner))
                                .append(Component.text("!", NamedTextColor.YELLOW));
                        UtilMessage.simpleMessage(player, "Voting", winText);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                        UtilItem.insert(player, winner);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(me.mykindos.betterpvp.core.Core.getPlugin(me.mykindos.betterpvp.core.Core.class), 0, 1);
    }

    private void roll() {
        if (lootTable.getWeightedLoot().isEmpty()) return;

        for (int i = 0; i < 8; i++) {
            rollItems.set(i, rollItems.get(i + 1));
        }

        rollItems.set(8, createRollItem(getRandomWeightedLoot()));

        for (int i = 0; i < 9; i++) {
            ItemStack item = rollItems.get(i).displayItem();
            setSlotElement(MIDDLE_ROW_START + i, new SlotElement.ItemSlotElement(new SimpleItem(new ItemWrapper(item))));
        }
    }
}
