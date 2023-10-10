package me.mykindos.betterpvp.core.gamer.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.exceptions.NoSuchGamerException;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Singleton
public class BalanceCommand extends Command {

    private final GamerManager gamerManager;

    @Inject
    public BalanceCommand(GamerManager gamerManager) {
        this.gamerManager = gamerManager;
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
        Gamer gamer = gamerManager.getObject(player.getUniqueId()).orElseThrow(() -> new NoSuchGamerException(player.getUniqueId()));
        UtilMessage.simpleMessage(player, "Economy", "<yellow>Balance: <green>$%,d", gamer.getProperty(GamerProperty.BALANCE).orElse(0));
    }

    @Singleton
    @SubCommand(BalanceCommand.class)
    private static class PayBalanceSubCommand extends Command {

        private final GamerManager gamerManager;

        @Inject
        public PayBalanceSubCommand(GamerManager gamerManager) {
            this.gamerManager = gamerManager;
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

            Gamer gamer = gamerManager.getObject(player.getUniqueId()).orElseThrow(() -> new NoSuchGamerException(player.getUniqueId()));

            Optional<Gamer> gamerOptional = gamerManager.getGamerByName(args[0]);
            if (gamerOptional.isEmpty()) {
                UtilMessage.simpleMessage(player, "Economy", "Could not find a player with the name <yellow>%s", args[0]);
                return;
            }

            Gamer targetGamer = gamerOptional.get();
            if(targetGamer.equals(gamer)) {
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

                UtilMessage.simpleMessage(player, "Economy", "You paid <yellow>%s <green>$%,d<gray>.", targetGamer.getClient().getName(), amountToPay);

                Player targetPlayer = Bukkit.getPlayer(UUID.fromString(targetGamer.getUuid()));
                if(targetPlayer != null) {
                    UtilMessage.simpleMessage(targetPlayer, "Economy", "You received <green>$%,d <gray>from <yellow>%s<gray>.", amountToPay, player.getName());
                }

                log.info("{} paid {} ${}", player.getName(), targetGamer.getClient().getName(), amountToPay);

            } catch (NumberFormatException ex) {
                UtilMessage.message(player, "Economy", "Value provided is not a valid number.");
            }
        }

        @Override
        public String getArgumentType(int arg) {
            return arg == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

    @Singleton
    @SubCommand(BalanceCommand.class)
    private static class GiveBalanceSubCommand extends Command {

        private final GamerManager gamerManager;

        @Inject
        public GiveBalanceSubCommand(GamerManager gamerManager) {
            this.gamerManager = gamerManager;
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

            Optional<Gamer> gamerOptional = gamerManager.getGamerByName(args[0]);
            if (gamerOptional.isEmpty()) {
                UtilMessage.simpleMessage(player, "Economy", "Could not find a player with the name <yellow>%s", args[0]);
                return;
            }

            try {
                int amountToPay = Integer.parseInt(args[1]);

                Gamer targetGamer = gamerOptional.get();
                targetGamer.saveProperty(GamerProperty.BALANCE, targetGamer.getBalance() + amountToPay);

                UtilMessage.simpleMessage(player, "Economy", "You gave <yellow>%s <green>$%d<gray>.", targetGamer.getClient().getName(), amountToPay);

                Player targetPlayer = Bukkit.getPlayer(UUID.fromString(targetGamer.getUuid()));
                if(targetPlayer != null) {
                    UtilMessage.simpleMessage(targetPlayer, "Economy", "You received <green>$%d <gray>from <yellow>%s<gray>.", amountToPay, player.getName());
                }

                log.info("{} gave {} ${}", player, targetGamer.getClient().getName(), amountToPay);

            } catch (NumberFormatException ex) {
                UtilMessage.message(player, "Economy", "Value provided is not a valid number.");
            }
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
