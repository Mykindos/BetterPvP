package me.mykindos.betterpvp.progression.profession.woodcutting.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.TreeCompactor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@CustomLog
@Singleton
public class TreeCompactorCommand extends Command {
    private final TreeCompactor treeCompactor;
    private final ItemHandler itemHandler;

    @Inject
    public TreeCompactorCommand(TreeCompactor treeCompactor, ItemHandler itemHandler) {
        this.treeCompactor = treeCompactor;
        this.itemHandler = itemHandler;
    }

    @Override
    public @NotNull String getName() {
        return "treecompactor";
    }

    @Override
    public @NotNull String getDescription() {
        return "Compactor your logs!!!";
    }

    public Component getUsage() {
        return UtilMessage.deserialize("<yellow>Usage</yellow>: <green>treecompactor <logType>");
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (!treeCompactor.doesPlayerHaveSkill(player)) return;

        // Only 1 argument is acceptable
        if (args.length != 1) {
            UtilMessage.message(player, "Command", getUsage());
            return;
        }

        int logsAfterCompaction = 0;

        while (UtilInventory.contains(player, Material.OAK_LOG,  64)) {
            UtilInventory.remove(player, Material.OAK_LOG, 64);

            BPvPItem item = itemHandler.getItem("progression:compacted_log");
            ItemStack itemStack = itemHandler.updateNames(item.getItemStack());
            itemHandler.updateNames(itemStack);

            player.getInventory().addItem(itemStack);
            logsAfterCompaction++;
        }

        UtilMessage.simpleMessage(player, "Progression", "Compacted your oak logs " + logsAfterCompaction + "x");

        log.info("{} compacted {}x logs.", player.getName(), logsAfterCompaction)
                .addClientContext(player).addLocationContext(player.getLocation()).submit();
    }

}
