package me.mykindos.betterpvp.core.logging.commands;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import me.mykindos.betterpvp.core.logging.UUIDLogger;
import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import me.mykindos.betterpvp.core.logging.type.UUIDType;
import me.mykindos.betterpvp.core.logging.type.logs.ItemLog;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@CustomLog
@Singleton
@SubCommand(LogCommand.class)
public class LegendLogSubcommand extends Command {

    private final ClientManager clientManager;
    private final UUIDManager uuidManager;

    @Inject
    public LegendLogSubcommand(ClientManager clientManager, UUIDManager uuidManager) {
        this.clientManager = clientManager;
        this.uuidManager = uuidManager;
    }

    @Override
    public String getName() {
        return "legend";
    }

    @Override
    public String getDescription() {
        return "Generate a manual log";
    }

    public Component getUsage() {
        return UtilMessage.deserialize("<green>Usage: /log <UUID> <player|null> <message>");
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 3) {
            log.info(String.valueOf(args.length));
            UtilMessage.message(player, "Log", getUsage());
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            UtilMessage.message(player, "Log", UtilMessage.deserialize("<light_purple>%s</light_purple> is not a valid UUID.", args[0]));
            return;
        }

        String message = String.join(" ", Arrays.stream(args).toList().subList(2, args.length));

        if (!args[1].equalsIgnoreCase("null")) {
            clientManager.search().offline(args[1], clientOptional -> {
                if (clientOptional.isEmpty()) {
                    UtilMessage.message(player, "Search", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid Player.", args[1]));
                    return;
                }
                run(player, uuid, clientOptional.get(), message);
            });
        } else {
            run(player, uuid, null, message);
        }



    }

    public void run(Player player, UUID uuid, @Nullable Client client, String message) {
        String finalMessage = "(" + uuid.toString() + ") " + message;
        UUID logID = log.info(finalMessage);
        ItemLog itemLog = (ItemLog) new ItemLog(logID, UUIDLogType.ITEM_CUSTOM, uuid)
                .addMeta(player.getUniqueId(), UUIDType.PLAYER1);
        if (client != null) {
            itemLog.addMeta(client.getUniqueId(), UUIDType.PLAYER2);
        }
        UUIDLogger.addItemLog(itemLog);
        clientManager.sendMessageToRank("Log", UtilMessage.deserialize("<yellow>%s</yellow> Generated a custom legend log: " + finalMessage, player.getName()), Rank.HELPER);
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return "ITEMUUID";
        }
        if (argCount == 2) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }


    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        //explicitly add null if it is a player
        List<String> tabCompletions = new ArrayList<>();

        if (args.length == 0) return super.processTabComplete(sender, args);

        if (getArgumentType(args.length).equals("PLAYER")) {
            tabCompletions.add("null");
        }

        String lowercaseArg = args[args.length - 1].toLowerCase();
        if (getArgumentType(args.length).equals("ITEMUUID")) {
            tabCompletions.addAll(uuidManager.getObjects().keySet().stream()
                    .filter(uuid -> uuid.toLowerCase().contains(lowercaseArg)).toList());
        }
        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }
}
