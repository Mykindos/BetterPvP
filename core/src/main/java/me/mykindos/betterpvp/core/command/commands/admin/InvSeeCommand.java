package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.menus.PlayerInventoryMenu;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class InvSeeCommand extends Command {

    private final ClientManager clientManager;
    private final ItemHandler itemHandler;

    @Inject
    public InvSeeCommand(ClientManager clientManager, ItemHandler itemHandler) {
        this.clientManager = clientManager;
        this.itemHandler = itemHandler;
        aliases.add("openinv");
    }

    @Override
    public String getName() {
        return "invsee";
    }

    @Override
    public String getDescription() {
        return "Opens a target players inventory";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.simpleMessage(player, "Usage: /invsee <player>");
            return;
        }

        clientManager.search().offline(args[0]).thenAcceptAsync(clientOptional -> {
            if (clientOptional.isEmpty()) {
                UtilMessage.simpleMessage(player, "Could not find player <yellow>" + args[0]);
                return;
            }

            final Client target = clientOptional.get();
            final Gamer targetGamer = target.getGamer();
            final Player targetPlayer = targetGamer.getPlayer();
            if (targetPlayer != null) {
                UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                    new PlayerInventoryMenu(itemHandler, targetPlayer, targetPlayer.getName(), targetPlayer.getUniqueId(), (CraftInventoryPlayer) targetPlayer.getInventory(), false).show(player);
                });
                return;
            }

            CraftInventoryPlayer playerInventory = UtilInventory.getOfflineInventory(target.getName(), target.getUniqueId());
            UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                new PlayerInventoryMenu(itemHandler, null, target.getName(), target.getUniqueId(), playerInventory, true).show(player);
            });

        });

    }
    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
