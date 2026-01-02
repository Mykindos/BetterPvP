package me.mykindos.betterpvp.core.item.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.attunement.GuiAttunement;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import me.mykindos.betterpvp.core.item.runeslot.RuneSlotDistributionRegistry;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Admin command to attune or unattune items.
 *
 * Usage:
 *   /attune - Opens the attunement GUI
 *   /attune set <true|false> - Set attuned state for held item
 *   /attune toggle - Toggle attuned state for held item
 *   /attune set <true|false> --all - Set attuned state for all items in inventory
 *   /attune toggle --all - Toggle attuned state for all items in inventory
 *
 * Permission: Admin only
 */
@Singleton
public class AttunementCommand extends Command {

    private final ItemFactory itemFactory;
    private final RuneSlotDistributionRegistry runeSlotRegistry;

    @Inject
    public AttunementCommand(ItemFactory itemFactory, RuneSlotDistributionRegistry runeSlotRegistry) {
        this.itemFactory = itemFactory;
        this.runeSlotRegistry = runeSlotRegistry;
    }

    @Override
    public String getName() {
        return "attune";
    }

    @Override
    public String getDescription() {
        return "Admin command to attune/unattune items";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        // No args -> open attunement menu
        if (args.length == 0) {
            GuiAttunement gui = new GuiAttunement(player, itemFactory, runeSlotRegistry);
            gui.show(player);
            return;
        }

        // Check for --all flag
        boolean processAll = args[args.length - 1].equalsIgnoreCase("--all");
        String[] commandArgs = processAll ? java.util.Arrays.copyOf(args, args.length - 1) : args;

        // Validate we have a subcommand after removing --all
        if (commandArgs.length == 0) {
            UtilMessage.message(player, "Attunement", "<yellow>Usage: /attune <set|toggle> [true|false] [--all]");
            return;
        }

        if (processAll) {
            // Process all items in inventory
            processAllItems(player, commandArgs);
        } else {
            // Process held item only
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem.getType().isAir()) {
                UtilMessage.message(player, "Attunement", "<red>You must be holding an item!");
                return;
            }
            processItem(player, heldItem, commandArgs, -1);
        }
    }

    /**
     * Process all items in the player's inventory
     */
    private void processAllItems(Player player, String[] args) {
        int processedCount = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                if (processItem(player, item, args, i)) {
                    processedCount++;
                }
            }
        }

        if (processedCount == 0) {
            UtilMessage.message(player, "Attunement", "<red>No items with purity found in inventory!");
        } else {
            UtilMessage.message(player, "Attunement", "<green>Processed <yellow>%d<green> items!", processedCount);
        }
    }

    /**
     * Process a single item and optionally update inventory slot
     *
     * @param player The player
     * @param item The item to process
     * @param args Command arguments
     * @param slot The inventory slot to update, or -1 for main hand
     * @return true if item was processed successfully
     */
    private boolean processItem(Player player, ItemStack item, String[] args, int slot) {
        // Convert to ItemInstance
        Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(item);
        if (instanceOpt.isEmpty()) {
            if (slot == -1) {
                UtilMessage.message(player, "Attunement", "<red>Could not read item!");
            }
            return false;
        }

        ItemInstance instance = instanceOpt.get();

        // Check for PurityComponent
        Optional<PurityComponent> purityOpt = instance.getComponent(PurityComponent.class);
        if (purityOpt.isEmpty()) {
            if (slot == -1) {
                UtilMessage.message(player, "Attunement", "<red>This item does not have purity!");
            }
            return false;
        }

        PurityComponent purity = purityOpt.get();

        // Parse subcommand
        boolean newAttunedState;
        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "toggle" -> {
                newAttunedState = !purity.isAttuned();
            }
            case "set" -> {
                if (args.length < 2) {
                    UtilMessage.message(player, "Attunement", "<yellow>Usage: /attune set <true|false> [--all]");
                    return false;
                }
                newAttunedState = Boolean.parseBoolean(args[1]);
            }
            default -> {
                UtilMessage.message(player, "Attunement", "<red>Unknown subcommand. Use 'set' or 'toggle'");
                return false;
            }
        }

        // Update purity component
        PurityComponent newPurity = purity.withAttuned(newAttunedState);
        ItemInstance newInstance = instance.withComponent(newPurity);

        // Replace item in inventory
        if (slot == -1) {
            player.getInventory().setItemInMainHand(newInstance.createItemStack());
            String state = newAttunedState ? "attuned" : "unattuned";
            UtilMessage.message(player, "Attunement", "<green>Item is now <yellow>%s<green>!", state);
        } else {
            player.getInventory().setItem(slot, newInstance.createItemStack());
        }

        return true;
    }
}
