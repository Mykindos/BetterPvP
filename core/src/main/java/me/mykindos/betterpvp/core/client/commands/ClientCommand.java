package me.mykindos.betterpvp.core.client.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        return "core.command.client.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.subcommand.required");
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
        return "core.command.admin.description";
    }

        @Override
        public void execute(Player player, Client client, String[] args) {
            client.setAdministrating(!client.isAdministrating());
            new ClientAdministrateEvent(client, player, client.isAdministrating()).callEvent();
            this.clientManager.save(client);

            Component status = client.isAdministrating() ? Translations.component("core.command.client.status.enabled").color(NamedTextColor.GREEN)
                    : Translations.component("core.command.client.status.disabled").color(NamedTextColor.RED);
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.admin.status", status);
            Component message = Component.text(player.getName(), NamedTextColor.YELLOW)
                    .append(Component.space())
                    .append(status)
                    .append(Component.space())
                    .append(Translations.component("core.command.client.admin.mode_suffix").color(NamedTextColor.GRAY));
            clientManager.sendMessageToRank("core.prefix.core", message, Rank.TRIAL_MOD);
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
        return "core.command.client-search.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length != 1) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.search.name_required");
                return;
            }


            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetOptional -> {
                targetOptional.ifPresentOrElse(target -> {
                    List<Component> result = new ArrayList<>();
                    Component nameStyled = UtilMessage.deserialize("<alt2>%s</alt2>", target.getName());
                    result.add(Translations.component("core.command.client.search.header", nameStyled));

                    List<String> previousNames = clientManager.getSqlLayer().getPreviousNames(target);
                    if (!previousNames.isEmpty()) {
                        result.add(Translations.component("core.command.client.search.previous_names", Component.text(String.join(", ", previousNames), NamedTextColor.WHITE)).color(NamedTextColor.YELLOW));
                    }
                    String totalTimePlayed = UtilTime.humanReadableFormat(Duration.ofMillis((Long) target.getProperty(ClientProperty.TIME_PLAYED).orElse(0L)));
                    String seasonTimePlayed = UtilTime.humanReadableFormat(Duration.ofMillis((Long) target.getGamer().getProperty(GamerProperty.TIME_PLAYED).orElse(0L)));
                    result.add(Translations.component("core.command.client.search.playtime_total", Component.text(totalTimePlayed, NamedTextColor.WHITE)).color(NamedTextColor.YELLOW));
                    result.add(Translations.component("core.command.client.search.playtime_season", Component.text(seasonTimePlayed, NamedTextColor.WHITE)).color(NamedTextColor.YELLOW));

                    ClientSearchEvent searchEvent = UtilServer.callEvent(new ClientSearchEvent(target));
                    searchEvent.getAdditionalData().forEach((key, value) -> {
                        result.add(Component.text(key + ": ", NamedTextColor.YELLOW).append(Component.text(value.toString(), NamedTextColor.WHITE)));
                    });
                    result.forEach(message -> UtilMessage.message(player, message));
                }, () -> {
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.search.not_found");
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
        return "core.command.promote.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.target.required");
                return;
            }

            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetOptional -> {
                if (targetOptional.isPresent()) {
                    Client targetClient = targetOptional.get();
                    Rank targetRank = Rank.getRank(targetClient.getRank().getId() + 1);
                    if (targetRank != null) {
                        if (client.getRank().getId() < targetRank.getId() || player.isOp()) {
                            targetClient.setRank(targetRank);
                            if (targetRank.equals(Rank.ADMIN)) {
                                targetClient.saveProperty(ClientProperty.SHOW_TAG, Rank.ShowTag.NONE.name());
                            } else {
                                targetClient.saveProperty(ClientProperty.SHOW_TAG, Rank.ShowTag.SHORT.name());
                            }

                            final Component nameStyled = UtilMessage.deserialize("<alt2>%s</alt2>", targetClient.getName());
                            UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.promote.success",
                                    nameStyled, targetRank.getTag(Rank.ShowTag.LONG, true));
                            clientManager.save(targetClient);

                            Component staffMessage = Translations.component("core.command.client.promote.staff_broadcast",
                                    Component.text(player.getName(), NamedTextColor.YELLOW),
                                    Component.text(targetClient.getName(), NamedTextColor.YELLOW),
                                    targetRank.getTag(Rank.ShowTag.LONG, true));
                            clientManager.sendMessageToRank("core.prefix.client", staffMessage, Rank.TRIAL_MOD);
                        } else {
                            UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.promote.not_allowed");
                        }
                    } else {
                        UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.promote.highest",
                                Component.text(targetClient.getName(), NamedTextColor.YELLOW));
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
    private static class DemoteSubCommand extends Command {

        @Inject
        private ClientManager clientManager;

        @Override
        public String getName() {
            return "demote";
        }

        @Override
        public String getDescription() {
        return "core.command.demote.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.target.required");
                return;
            }

            clientManager.search(player).offline(args[0]).thenAcceptAsync(targetOptional -> {
                if (targetOptional.isEmpty()) {
                    return;
                }

                Client targetClient = targetOptional.get();

                // Prevent demoting this specific UUID unless self-demote
                if (targetClient.getUuid().equalsIgnoreCase("e1f5d06b-685b-46a0-b22c-176d6aefffff")
                        && !client.getUuid().equalsIgnoreCase(targetClient.getUuid())) {
                    return;
                }

                Rank targetRank = Rank.getRank(targetClient.getRank().getId() - 1);
                if (targetRank == null) {
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.demote.lowest",
                            Component.text(targetClient.getName(), NamedTextColor.YELLOW));
                    return;
                }

                if (client.getRank().getId() < targetRank.getId() || player.isOp()) {
                    targetClient.setRank(targetRank);
                    if (targetRank.equals(Rank.ADMIN)) {
                        targetClient.saveProperty(ClientProperty.SHOW_TAG, Rank.ShowTag.NONE.name());
                    } else {
                        targetClient.saveProperty(ClientProperty.SHOW_TAG, Rank.ShowTag.SHORT.name());
                    }
                    final Component nameStyled = UtilMessage.deserialize("<alt2>%s</alt2>", targetClient.getName());
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.demote.success",
                            nameStyled, targetRank.getTag(Rank.ShowTag.LONG, true));
                    clientManager.save(targetClient);

                    Component staffMessage = Translations.component("core.command.client.demote.staff_broadcast",
                            Component.text(player.getName(), NamedTextColor.YELLOW),
                            Component.text(targetClient.getName(), NamedTextColor.YELLOW),
                            targetRank.getTag(Rank.ShowTag.LONG, true));
                    clientManager.sendMessageToRank("core.prefix.client", staffMessage, Rank.TRIAL_MOD);
                } else {
                    UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.demote.not_allowed");
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
        return "core.command.count.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.count",
                        Component.text(clientManager.getSqlLayer().getTotalClients(), NamedTextColor.YELLOW));
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
        return "core.command.set-media-channel.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.client.target.required");
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
