package me.mykindos.betterpvp.core.client.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClientCommand extends Command {

    @Override
    public String getName() {
        return "client";
    }

    @Override
    public String getDescription() {
        return "Base client command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Command", "You must specify a sub command");
    }

    @Singleton
    @SubCommand(ClientCommand.class)
    private static class AdminSubCommand extends Command {

        @Override
        public String getName() {
            return "admin";
        }

        @Override
        public String getDescription() {
            return "Enable administration mode";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            client.setAdministrating(!client.isAdministrating());
            
            Component status = client.isAdministrating() ? Component.text("enabled", NamedTextColor.GREEN) : Component.text("disabled", NamedTextColor.RED);
            UtilMessage.simpleMessage(player, "Command", Component.text("Client admin: ").append(status));
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

    @Singleton
    @SubCommand(ClientCommand.class)
    private static class SearchSubCommand extends Command {

        @Inject
        private ClientManager clientManager;

        @Override
        public String getName() {
            return "search";
        }

        @Override
        public String getDescription() {
            return "Search for a client by name";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length != 1) {
                UtilMessage.message(player, "Command", "You must provide a name to search");
                return;
            }

            String name = args[0];

            Optional<Client> clientOptional = clientManager.getClientByName(name);
            clientOptional.ifPresentOrElse(target -> {
                List<Component> result = new ArrayList<>();
                result.add(UtilMessage.deserialize("<alt2>%s</alt2> Client Details", target.getName()));
                //result.add(Component.text("IP Address: ", NamedTextColor.YELLOW).append(Component.text(target.getIP(), NamedTextColor.GRAY)));
                //        + (client.hasRank(Rank.ADMIN) ? ChatColor.GRAY + target.getIP() : ChatColor.RED + "N/A"));
                //event.getResult().add(ChatColor.YELLOW + "Previous Name: " + ChatColor.GRAY + target.getOldName());
                //event.getResult().add(ChatColor.YELLOW + "IP Alias: " + ChatColor.GRAY + (client.hasRank(Rank.ADMIN, false)
                //        ? ClientUtilities.getDetailedIPAlias(target, false) : ClientUtilities.getDetailedIPAlias(target, true)));
                //event.getResult().add(ChatColor.YELLOW + "Rank: " + ChatColor.GRAY + UtilFormat.cleanString(target.getRank().toString()));
                //event.getResult().add(ChatColor.YELLOW + "Discord Linked: " + ChatColor.GRAY + target.isDiscordLinked());
                //event.getResult().add(ChatColor.YELLOW + "Punishments: " + ChatColor.GRAY + punishments);

                result.forEach(message -> UtilMessage.message(player, message));

            }, () -> UtilMessage.message(player, "Command", "Could not find a client with this name"));
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

    @Singleton
    @SubCommand(ClientCommand.class)
    private static class PromoteSubCommand extends Command {

        @Inject
        private ClientManager clientManager;

        @Override
        public String getName() {
            return "promote";
        }

        @Override
        public String getDescription() {
            return "Promote a client to a higher rank";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, "Client", "You must specify a client");
                return;
            }

            Optional<Client> clientOptional = clientManager.getClientByName(args[0]);
            if (clientOptional.isPresent()) {
                Client targetClient = clientOptional.get();
                Rank targetRank = Rank.getRank(targetClient.getRank().getId() + 1);
                if(targetRank != null) {
                    if (client.getRank().getId() < targetRank.getId() || player.isOp()) {
                        targetClient.setRank(targetRank);

                        final Component msg = UtilMessage.deserialize("<alt2>%s</alt2> has been promoted to ", targetClient.getName()).append(targetRank.getTag(true));
                        UtilMessage.simpleMessage(player, "Client", msg);
                        clientManager.getRepository().save(targetClient);
                    }else{
                        UtilMessage.message(player, "Client", "You cannot promote someone to your current rank or higher.");
                    }
                }else{
                    UtilMessage.simpleMessage(player, "Client", "<alt2>%s</alt2> already has the highest rank.", targetClient.getName());
                }
            }
        }
    }

    @Singleton
    @SubCommand(ClientCommand.class)
    private static class DemoteSubCommand extends Command {

        @Inject
        private ClientManager clientManager;

        @Override
        public String getName() {
            return "demote";
        }

        @Override
        public String getDescription() {
            return "Demote a client to a higher rank";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, "Client", "You must specify a client");
                return;
            }

            Optional<Client> clientOptional = clientManager.getClientByName(args[0]);
            if (clientOptional.isPresent()) {
                Client targetClient = clientOptional.get();
                Rank targetRank = Rank.getRank(targetClient.getRank().getId() - 1);
                if(targetRank != null) {
                    if (client.getRank().getId() < targetRank.getId() || player.isOp()) {
                        targetClient.setRank(targetRank);

                        final Component msg = UtilMessage.deserialize("<alt2>%s</alt2> has been demoted to ", targetClient.getName()).append(targetRank.getTag(true));
                        UtilMessage.simpleMessage(player, "Client", msg);
                        clientManager.getRepository().save(targetClient);
                    }else{
                        UtilMessage.message(player, "Client", "You cannot demote someone that is higher rank than you.");
                    }
                }else{
                    UtilMessage.simpleMessage(player, "Client", "<alt2>%s</alt2> already has the lowest rank.", targetClient.getName());
                }
            }
        }
    }
}
