package me.mykindos.betterpvp.core.client.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientAdministrateEvent;
import me.mykindos.betterpvp.core.client.events.ClientSearchEvent;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

        @Inject
        private ClientManager clientManager;

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
            new ClientAdministrateEvent(client, player, client.isAdministrating()).callEvent();
            this.clientManager.save(client);

            Component status = client.isAdministrating() ? Component.text("enabled", NamedTextColor.GREEN) : Component.text("disabled", NamedTextColor.RED);
            UtilMessage.simpleMessage(player, "Command", Component.text("Client admin: ").append(status));
            Component message = Component.text(player.getName(), NamedTextColor.YELLOW).append(Component.space()).append(status).append(Component.text(" client administration mode", NamedTextColor.GRAY));
            clientManager.sendMessageToRank("Core", message, Rank.TRIAL_MOD);
            player.updateCommands();
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
        @Config(path = "core.salt", defaultValue = "")
        private String salt;

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


            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetOptional -> {
                targetOptional.ifPresentOrElse(target -> {
                    List<Component> result = new ArrayList<>();
                    result.add(UtilMessage.deserialize("<alt2>%s</alt2> Client Details", target.getName()));

                    List<String> previousNames = clientManager.getSqlLayer().getPreviousNames(target);
                    if (!previousNames.isEmpty()) {
                        result.add(UtilMessage.deserialize("<yellow>Previous names: <white>%s", String.join("<gray>, <white>", previousNames)));
                    }
                    String totalTimePlayed = UtilTime.humanReadableFormat(Duration.ofMillis((Long) target.getProperty(ClientProperty.TIME_PLAYED).orElse(0L)));
                    String seasonTimePlayed = UtilTime.humanReadableFormat(Duration.ofMillis((Long) target.getGamer().getProperty(GamerProperty.TIME_PLAYED).orElse(0L)));
                    result.add(UtilMessage.deserialize("<yellow>Play time (Total): <white>%s", totalTimePlayed));
                    result.add(UtilMessage.deserialize("<yellow>Play time (Season): <white>%s", seasonTimePlayed));

                    ClientSearchEvent searchEvent = UtilServer.callEvent(new ClientSearchEvent(target));
                    searchEvent.getAdditionalData().forEach((key, value) -> {
                        result.add(UtilMessage.deserialize("<yellow>%s: <white>%s", key, value));
                    });
                    result.forEach(message -> UtilMessage.message(player, message));
                }, () -> {
                    UtilMessage.message(player, "Command", "Could not find a client with this name");
                });
            });


        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

    @CustomLog
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

            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetOptional -> {
                if (targetOptional.isPresent()) {
                    Client targetClient = targetOptional.get();
                    Rank targetRank = Rank.getRank(targetClient.getRank().getId() + 1);
                    if (targetRank != null) {
                        if (client.getRank().getId() < targetRank.getId() || player.isOp()) {
                            targetClient.setRank(targetRank);
                            if (targetRank.equals(Rank.MINEPLEX)) {
                                targetClient.saveProperty(ClientProperty.SHOW_TAG, Rank.ShowTag.NONE.name());
                            } else {
                                targetClient.saveProperty(ClientProperty.SHOW_TAG, Rank.ShowTag.SHORT.name());
                            }

                            final Component msg = UtilMessage.deserialize("<alt2>%s</alt2> has been promoted to ", targetClient.getName()).append(targetRank.getTag(Rank.ShowTag.LONG, true));
                            UtilMessage.simpleMessage(player, "Client", msg);
                            clientManager.save(targetClient);

                            Component staffMessage = UtilMessage.deserialize("<yellow>%s</yellow> has promoted <yellow>%s</yellow> to ", player.getName(), targetClient.getName()).append(targetRank.getTag(Rank.ShowTag.LONG, true));
                            clientManager.sendMessageToRank("Client", staffMessage, Rank.TRIAL_MOD);

                            Player target = Bukkit.getPlayer(targetClient.getUniqueId());
                            if (target != null) {
                                target.updateCommands();
                            }
                        } else {
                            UtilMessage.message(player, "Client", "You cannot promote someone to your current rank or higher.");
                        }
                    } else {
                        UtilMessage.simpleMessage(player, "Client", "<alt2>%s</alt2> already has the highest rank.", targetClient.getName());
                    }
                }
            });

        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

    @CustomLog
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
            return "Demote a client to a lower rank";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, "Client", "You must specify a client");
                return;
            }

            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetOptional -> {
                if (targetOptional.isPresent()) {
                    Client targetClient = targetOptional.get();
                    Rank formerRank = targetClient.getRank();
                    Rank targetRank = Rank.getRank(targetClient.getRank().getId() - 1);
                    if (targetRank != null) {
                        if (client.getRank().getId() < targetRank.getId() || player.isOp()) {
                            targetClient.setRank(targetRank);
                            if (targetRank.equals(Rank.MINEPLEX)) {
                                targetClient.saveProperty(ClientProperty.SHOW_TAG, Rank.ShowTag.NONE.name());
                            } else {
                                targetClient.saveProperty(ClientProperty.SHOW_TAG, Rank.ShowTag.SHORT.name());
                            }
                            final Component msg = UtilMessage.deserialize("<alt2>%s</alt2> has been demoted to ", targetClient.getName()).append(targetRank.getTag(Rank.ShowTag.LONG, true));
                            UtilMessage.simpleMessage(player, "Client", msg);
                            clientManager.save(targetClient);

                            Component staffMessage = UtilMessage.deserialize("<yellow>%s</yellow> has demoted <yellow>%s</yellow> to ", player.getName(), targetClient.getName()).append(targetRank.getTag(Rank.ShowTag.LONG, true));
                            clientManager.sendMessageToRank("Client", staffMessage, Rank.TRIAL_MOD);

                            Player target = Bukkit.getPlayer(targetClient.getUniqueId());
                            if (target != null) {
                                target.updateCommands();
                            }
                        } else {
                            UtilMessage.message(player, "Client", "You cannot demote someone that is higher rank than you.");
                        }
                    } else {
                        UtilMessage.simpleMessage(player, "Client", "<alt2>%s</alt2> already has the lowest rank.", targetClient.getName());
                    }
                }
            });

        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

    @Singleton
    @SubCommand(ClientCommand.class)
    private static class CountSubCommand extends Command {

        @Inject
        private ClientManager clientManager;

        @Override
        public String getName() {
            return "count";
        }

        @Override
        public String getDescription() {
            return "See total client count";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
                UtilMessage.simpleMessage(player, "Client", "Total clients: " + clientManager.getSqlLayer().getTotalClients());
            });

        }

    }

    @Singleton
    @SubCommand(ClientCommand.class)
    private static class SetMediaChannelSubCommand extends Command {

        @Inject
        private ClientManager clientManager;

        @Override
        public String getName() {
            return "setmediachannel";
        }

        @Override
        public String getDescription() {
            return "Set the media channel for a client (URL)";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, "Client", "You must specify a client");
                return;
            }

            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetOptional -> {
                if (targetOptional.isPresent()) {
                    Client targetClient = targetOptional.get();
                    targetClient.saveProperty(ClientProperty.MEDIA_CHANNEL, args.length == 2 ? args[1] : "");
                }
            });

        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

}
