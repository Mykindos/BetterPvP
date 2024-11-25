package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentHandler;
import me.mykindos.betterpvp.core.client.punishments.menu.PunishmentItem;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.List;

@Singleton
public class PlayerPunishmentHistoryCommand extends Command {
    private final PunishmentHandler punishmentHandler;
    @Inject
    public PlayerPunishmentHistoryCommand(PunishmentHandler punishmentHandler) {
        this.punishmentHandler = punishmentHandler;
        aliases.addAll(List.of(
                "punishhistory",
                "ph"
                ));
    }

    @Override
    public String getName() {
        return "punishmenthistory";
    }

    @Override
    public String getDescription() {
        return "Shows your own punishment history";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        List<Item> items = client.getPunishments().stream()
                .sorted(Comparator.comparingLong(Punishment::getExpiryTime).reversed())
                .map(punishment -> new PunishmentItem(punishment, punishmentHandler, false, null))
                .map(Item.class::cast).toList();
        UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
            new ViewCollectionMenu(client.getName() + "'s Punish History", items, null).show(player);
        });
    }
}
