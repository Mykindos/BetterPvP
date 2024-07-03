package me.mykindos.betterpvp.core.client.gamer.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

@CustomLog
@Singleton
public class BalanceCommand extends Command {

    @Inject
    public BalanceCommand() {
        aliases.addAll(List.of("bal", "money"));
    }

    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public String getDescription() {
        return "View your balance";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = client.getGamer();
        UtilMessage.simpleMessage(player, "Economy", "<yellow>Balance: <green>$%,d", gamer.getProperty(GamerProperty.BALANCE).orElse(0));
    }

    @Singleton
    @SubCommand(BalanceCommand.class)
    private static class PayBalanceSubCommand extends Command {

        private final ClientManager clientManager;

        @Inject
        public PayBalanceSubCommand(ClientManager clientManager) {
            this.clientManager = clientManager;
        }

        @Override
        public String getName() {
            return "pay";
        }

        @Override
        public String getDescription() {
            return "Pay money to another player";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length != 2) {
                UtilMessage.message(player, "Economy", "Correct usage /balance pay <player> <amount>");
                return;
            }

            final Gamer gamer = client.getGamer();
            clientManager.search(player).offline(args[0], targetClientOptional -> {
                if (targetClientOptional.isEmpty()) {
                    UtilMessage.message(player, "Economy", "There is no player with the name <yellow>%s</yellow>", args[0]);
                    return;
                }
                Client targetClient = targetClientOptional.get();
                final Gamer targetGamer = targetClient.getGamer();
                if (targetGamer.equals(gamer)) {
                    UtilMessage.message(player, "Economy", "You cannot send money to yourself.");
                    return;
                }

                try {

                    int amountToPay = Integer.parseInt(args[1]);
                    if(amountToPay > gamer.getBalance()) {
                        UtilMessage.message(player, "Economy", "You have insufficient funds to make a payment of this amount.");
                        return;
                    }

                    if(amountToPay <= 0) {
                        UtilMessage.message(player, "Economy", "You must specify a value greater than 0.");
                        return;
                    }

                    gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() - amountToPay);
                    targetGamer.saveProperty(GamerProperty.BALANCE, targetGamer.getBalance() + amountToPay);

                    UtilMessage.simpleMessage(player, "Economy", "You paid <yellow>%s <green>$%,d<gray>.", targetClient.getName(), amountToPay);

                    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(targetGamer.getUuid()));
                    if(targetPlayer != null) {
                        UtilMessage.simpleMessage(targetPlayer, "Economy", "You received <green>$%,d <gray>from <yellow>%s<gray>.", amountToPay, player.getName());
                    }

                    log.info("{} paid {} ${}", player.getName(), targetClient.getName(), amountToPay).submit();

                } catch (NumberFormatException ex) {
                    UtilMessage.message(player, "Economy", "Value provided is not a valid number.");
                }
            } );


        }

        @Override
        public String getArgumentType(int arg) {
            return arg == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

    @Singleton
    @SubCommand(BalanceCommand.class)
    private static class GiveBalanceSubCommand extends Command {

        private final ClientManager clientManager;

        @Inject
        public GiveBalanceSubCommand(ClientManager clientManager) {
            this.clientManager = clientManager;
        }

        @Override
        public String getName() {
            return "give";
        }

        @Override
        public String getDescription() {
            return "Give money to another player";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length != 2) {
                UtilMessage.message(player, "Economy", "Correct usage /balance give <player> <amount>");
                return;
            }

            clientManager.search(player).offline(args[0], targetClientOptional -> {
                if (targetClientOptional.isEmpty()) {
                    UtilMessage.message(player, "Economy", "There is no player with the name <yellow>%s</yellow>", args[0]);
                    return;
                }
                final Client targetClient = targetClientOptional.get();

                try {
                    int amountToPay = Integer.parseInt(args[1]);

                    final Gamer targetGamer = targetClient.getGamer();
                    targetGamer.saveProperty(GamerProperty.BALANCE, targetGamer.getBalance() + amountToPay);

                    UtilMessage.simpleMessage(player, "Economy", "You gave <yellow>%s <green>$%d<gray>.", targetClient.getName(), amountToPay);

                    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(targetGamer.getUuid()));
                    if (targetPlayer != null) {
                        UtilMessage.simpleMessage(targetPlayer, "Economy", "You received <green>$%,d <gray>from <yellow>%s<gray>.", amountToPay, player.getName());
                    }

                    log.info("{} gave {} ${}", player, targetClient.getName(), amountToPay).submit();

                } catch (NumberFormatException ex) {
                    UtilMessage.message(player, "Economy", "Value provided is not a valid number.");
                }
            });
        }

        @Override
        public String getArgumentType(int arg) {
            return arg == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

    @Singleton
    @SubCommand(BalanceCommand.class)
    private static class SetBalanceSubCommand extends Command {

        private final ClientManager clientManager;

        @Inject
        public SetBalanceSubCommand(ClientManager clientManager) {
            this.clientManager = clientManager;
        }

        @Override
        public String getName() {
            return "set";
        }

        @Override
        public String getDescription() {
            return "Set a player's balance";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length != 2) {
                UtilMessage.message(player, "Economy", "Correct usage /balance set <player> <amount>");
                return;
            }

            clientManager.search(player).offline(args[0], targetClientOptional -> {
                if (targetClientOptional.isEmpty()) {
                    UtilMessage.message(player, "Economy", "There is no player with the name <yellow>%s</yellow>", args[0]);
                    return;
                }
                final Client targetClient = targetClientOptional.get();

                try {
                    int amount = Integer.parseInt(args[1]);

                    final Gamer targetGamer = targetClient.getGamer();
                    targetGamer.saveProperty(GamerProperty.BALANCE, amount);

                    UtilMessage.simpleMessage(player, "Economy", "You set <yellow>%s<gray>'s balance to <green>$%,d<gray>.", targetClient.getName(), amount);

                    log.info("{} set {}'s balance to ${}", player, targetClient.getName(), amount).submit();

                } catch (NumberFormatException ex) {
                    UtilMessage.message(player, "Economy", "Value provided is not a valid number.");
                }
            });
        }

        @Override
        public String getArgumentType(int arg) {
            return arg == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

    @Singleton
    @SubCommand(BalanceCommand.class)
    private static class GetBalanceSubCommand extends Command {

        private final ClientManager clientManager;

        @Inject
        public GetBalanceSubCommand(ClientManager clientManager) {
            this.clientManager = clientManager;
        }

        @Override
        public String getName() {
            return "get";
        }

        @Override
        public String getDescription() {
            return "get a player's balance";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, "Economy", "Correct usage /balance get <player>");
                return;
            }

            clientManager.search(player).offline(args[0], targetClientOptional -> {
                if (targetClientOptional.isEmpty()) {
                    UtilMessage.message(player, "Economy", "There is no player with the name <yellow>%s</yellow>", args[0]);
                    return;
                }
                final Client targetClient = targetClientOptional.get();

                    final Gamer targetGamer = targetClient.getGamer();

                    UtilMessage.simpleMessage(player, "Economy", "<yellow>%s<gray> has <green>$%,d<gray>.", targetClient.getName(), targetGamer.getBalance());

            });
        }

        @Override
        public String getArgumentType(int arg) {
            return arg == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

}
