package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
@CustomLog
public class ClearCommand extends Command {

    private final ItemHandler itemHandler;
    private final ClientManager clientManager;

    @Inject
    public ClearCommand(ItemHandler itemHandler, ClientManager clientManager) {
        this.itemHandler = itemHandler;
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clear your inventory";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length == 0) {
            doClear(player, player);
        } else if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                doClear(player, target);
            }
        } else {
            UtilMessage.message(player, "CLear", "Usage: /clear [name], use /minecraft:clear for advanced usage");
        }
    }

    private void doClear(Player runner, Player target) {
        List<UUIDItem> uuidItems = itemHandler.getUUIDItems(target);
        Component successFeedback = UtilMessage.deserialize("<yellow>%s</yellow> cleared <yellow>%s</yellow>'s inventory",
                runner.getName(), target.getName());
        if (uuidItems.isEmpty()) {
            target.getInventory().clear();
            clientManager.sendMessageToRank("Clear", successFeedback, Rank.HELPER);
            return;
        }

        new ConfirmationMenu("Inventory has UUIDItems, confirm clear", (success) -> {
            if (Boolean.TRUE.equals(success)) {

                itemHandler.getUUIDItems(target).forEach(uuidItem -> {
                    log.info("{} cleared ({}) from {}'s inventory",
                            runner.getName(), uuidItem.getUuid(), target.getName())
                            .setAction("ITEM_CLEAR")
                            .addClientContext(runner)
                            .addItemContext(uuidItem)
                            .addClientContext(target, true)
                            .submit();
                });
                target.getInventory().clear();
                clientManager.sendMessageToRank("Clear", successFeedback, Rank.HELPER);
            }
        }).show(runner);
    }

    @Override
    public String getArgumentType(int args) {
        if (args == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }
}
