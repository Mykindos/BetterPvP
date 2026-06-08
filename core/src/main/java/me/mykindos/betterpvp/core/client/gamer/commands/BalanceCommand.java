package me.mykindos.betterpvp.core.client.gamer.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

@CustomLog
@Singleton
public class BalanceCommand extends Command {

    private static final String ECONOMY_PREFIX = "core.prefix.economy";

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
        return "core.command.balance.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = client.getGamer();
        final int balance = (Integer) gamer.getProperty(GamerProperty.BALANCE).orElse(0);
        UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.view",
                Component.text("$" + String.format("%,d", balance), NamedTextColor.GREEN));
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
        return "core.command.pay-balance.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length != 2) {
                UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.pay.usage");
                return;
            }

            final Gamer gamer = client.getGamer();
            int amountToPay;
            try {
                amountToPay = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.invalid_number");
                return;
            }

            if (amountToPay <= 0) {
                UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.amount_positive");
                return;
            }

            if (amountToPay > gamer.getBalance()) {
                UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.insufficient_funds");
                return;
            }

            Player targetPlayer = Bukkit.getPlayer(args[0]);
            if(targetPlayer == null || !targetPlayer.isOnline()) {
                UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.player_not_found",
                        Component.text(args[0], NamedTextColor.YELLOW));
                return;
            }

            clientManager.search(player).offline(args[0]).thenAccept(targetClientOptional -> {
                if (targetClientOptional.isEmpty()) {
                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.player_not_found",
                            Component.text(args[0], NamedTextColor.YELLOW));
                    return;
                }
                Client targetClient = targetClientOptional.get();
                if(!targetClient.isOnline()) {
                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.player_not_online",
                            Component.text(args[0], NamedTextColor.YELLOW));
                    return;
                }

                final Gamer targetGamer = targetClient.getGamer();
                if (targetGamer.equals(gamer)) {
                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.pay.self");
                    return;
                }

                UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                    if (amountToPay > gamer.getBalance()) {
                        UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.insufficient_funds");
                        return;
                    }

                    gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() - amountToPay);
                    targetGamer.saveProperty(GamerProperty.BALANCE, targetGamer.getBalance() + amountToPay);

                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.pay.success",
                            Component.text(targetClient.getName(), NamedTextColor.YELLOW),
                            Component.text("$" + String.format("%,d", amountToPay), NamedTextColor.GREEN));
                    UtilMessage.message(targetPlayer, ECONOMY_PREFIX, "core.command.balance.pay.received",
                            Component.text("$" + String.format("%,d", amountToPay), NamedTextColor.GREEN),
                            Component.text(player.getName(), NamedTextColor.YELLOW));

                    log.info("{} paid {} ${}", player.getName(), targetClient.getName(), amountToPay).submit();
                });
            });


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
        return "core.command.give-balance.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length != 2) {
                UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.give.usage");
                return;
            }

            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetClientOptional -> {
                if (targetClientOptional.isEmpty()) {
                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.player_not_found",
                            Component.text(args[0], NamedTextColor.YELLOW));
                    return;
                }
                final Client targetClient = targetClientOptional.get();

                try {
                    int amountToPay = Integer.parseInt(args[1]);

                    final Gamer targetGamer = targetClient.getGamer();
                    targetGamer.saveProperty(GamerProperty.BALANCE, targetGamer.getBalance() + amountToPay);

                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.give.success",
                            Component.text(targetClient.getName(), NamedTextColor.YELLOW),
                            Component.text("$" + String.format("%,d", amountToPay), NamedTextColor.GREEN));

                    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(targetGamer.getUuid()));
                    if (targetPlayer != null) {
                        UtilMessage.message(targetPlayer, ECONOMY_PREFIX, "core.command.balance.pay.received",
                                Component.text("$" + String.format("%,d", amountToPay), NamedTextColor.GREEN),
                                Component.text(player.getName(), NamedTextColor.YELLOW));
                    }

                    log.info("{} gave {} ${}", player, targetClient.getName(), amountToPay).submit();

                } catch (NumberFormatException ex) {
                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.invalid_number");
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
        return "core.command.set-balance.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length != 2) {
                UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.set.usage");
                return;
            }

            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetClientOptional -> {
                if (targetClientOptional.isEmpty()) {
                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.player_not_found",
                            Component.text(args[0], NamedTextColor.YELLOW));
                    return;
                }
                final Client targetClient = targetClientOptional.get();

                try {
                    int amount = Integer.parseInt(args[1]);

                    final Gamer targetGamer = targetClient.getGamer();
                    targetGamer.saveProperty(GamerProperty.BALANCE, amount);

                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.set.success",
                            Component.text(targetClient.getName(), NamedTextColor.YELLOW),
                            Component.text("$" + String.format("%,d", amount), NamedTextColor.GREEN));

                    log.info("{} set {}'s balance to ${}", player, targetClient.getName(), amount).submit();

                } catch (NumberFormatException ex) {
                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.invalid_number");
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
        return "core.command.get-balance.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length < 1) {
                UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.get.usage");
                return;
            }

            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetClientOptional -> {
                if (targetClientOptional.isEmpty()) {
                    UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.player_not_found",
                            Component.text(args[0], NamedTextColor.YELLOW));
                    return;
                }
                final Client targetClient = targetClientOptional.get();

                final Gamer targetGamer = targetClient.getGamer();

                UtilMessage.message(player, ECONOMY_PREFIX, "core.command.balance.get.success",
                        Component.text(targetClient.getName(), NamedTextColor.YELLOW),
                        Component.text("$" + String.format("%,d", targetGamer.getBalance()), NamedTextColor.GREEN));
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
