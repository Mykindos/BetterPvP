package me.mykindos.betterpvp.game.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.GameRegistry;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.listener.state.TransitionHandler;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.attribute.GameAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.GameAttributeManager;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@CustomLog
@Singleton
public class GameCommand extends Command {

    private final ServerController serverController;
    private final GameRegistry gameRegistry;
    private final GameAttributeManager attributeManager;

    @Inject
    public GameCommand(ClientManager clientManager, ServerController serverController, GameRegistry gameRegistry, GameAttributeManager attributeManager) {
        super();
        this.serverController = serverController;
        this.gameRegistry = gameRegistry;
        this.attributeManager = attributeManager;

        // Add aliases
        this.aliases.add("g");
    }

    @Override
    public String getName() {
        return "game";
    }

    @Override
    public String getDescription() {
        return "Game management commands";
    }

    @Override
    public void execute(Player player, Client client, String[] args) {
        UtilMessage.message(player, "Game", "Usage: <alt2>/game attribute <attribute> set <value>");
        UtilMessage.message(player, "Game", "Usage: <alt2>/game attribute <attribute> get");
        UtilMessage.message(player, "Game", "Usage: <alt2>/game attribute <attribute> reset");
        UtilMessage.message(player, "Game", "Usage: <alt2>/game set <game>");
        UtilMessage.message(player, "Game", "Usage: <alt2>/game start");
        UtilMessage.message(player, "Game", "Usage: <alt2>/game end");
    }

    @Singleton
    @SubCommand(GameCommand.class)
    public static class GameStartCommand extends Command {

        private final ServerController serverController;
        private final TransitionHandler transitionHandler;

        @Inject
        public GameStartCommand(ServerController serverController, TransitionHandler transitionHandler) {
            this.serverController = serverController;
            this.transitionHandler = transitionHandler;
        }

        @Override
        public String getName() {
            return "start";
        }

        @Override
        public String getDescription() {
            return "Start the game";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            if (serverController.getCurrentState() == GameState.IN_GAME || serverController.getCurrentState() == GameState.ENDING) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>You cannot start the game while in-game");
                return;
            }

            if (!transitionHandler.checkStateRequirements(true)) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>Cannot start the game, not enough players");
                return;
            }

            serverController.transitionTo(GameState.IN_GAME);
        }
    }

    @Singleton
    @SubCommand(GameCommand.class)
    public static class GameEndCommand extends Command {

        private final ServerController serverController;

        @Inject
        public GameEndCommand(ServerController serverController) {
            this.serverController = serverController;
        }

        @Override
        public String getName() {
            return "end";
        }

        @Override
        public String getDescription() {
            return "End the game";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            if (serverController.getCurrentState() != GameState.IN_GAME) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>You cannot end the game while not in-game");
                return;
            }

            serverController.getCurrentGame().forceEnd();
            serverController.transitionTo(GameState.ENDING);
        }
    }

    @Singleton
    @SubCommand(GameCommand.class)
    public static class GameSetCommand extends Command {

        private final ServerController serverController;
        private final GameRegistry gameRegistry;
        private final MapManager mapManager;

        @Inject
        public GameSetCommand(ServerController serverController, GameRegistry gameRegistry, MapManager mapManager) {
            this.serverController = serverController;
            this.gameRegistry = gameRegistry;
            this.mapManager = mapManager;
        }

        @Override
        public String getName() {
            return "set";
        }

        @Override
        public String getDescription() {
            return "Set the current game";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, "Game", "Usage: <alt2>/game set <game>");
                return;
            }

            if (serverController.getCurrentState() != GameState.WAITING) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>You can only set the game while in WAITING state");
                return;
            }

            String gameName = String.join(" ", args);
            final Optional<AbstractGame<?, ?>> foundOpt = gameRegistry.getRegisteredGames().stream()
                    .filter(game -> game.getConfiguration().getName().equalsIgnoreCase(gameName))
                    .findFirst();
            if (foundOpt.isEmpty()) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>Unknown game <alt>" + gameName + "</alt>");
                return;
            }

            // get a map
            final AbstractGame<?, ?> game = foundOpt.get();
            final Optional<MappedWorld> mapOpt = mapManager.selectRandomMap(game);
            if (mapOpt.isEmpty()) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>No maps found for game <alt>" + gameName + "</alt>");
                return;
            }

            if (serverController.getCurrentGame() == game) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>Game is already set to <alt>" + gameName + "</alt>");
                return;
            }

            // set
            serverController.setGame(game);
            mapManager.setCurrentMap(mapOpt.get());
            UtilMessage.message(player, "Game", "Game set to <alt>" + gameName + "</alt>");
            SoundEffect.HIGH_PITCH_PLING.play(player);
        }

        @Override
        public List<String> processTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return gameRegistry.getRegisteredGames().stream()
                        .map(game -> game.getConfiguration().getName())
                        .toList();
            } else {
                return super.processTabComplete(sender, args);
            }
        }
    }

    @Singleton
    @SubCommand(GameCommand.class)
    public static class GameAttributeCommand extends Command {

        private final GameAttributeManager attributeManager;

        @Inject
        public GameAttributeCommand(GameAttributeManager attributeManager) {
            this.attributeManager = attributeManager;
        }

        @Override
        public String getName() {
            return "attribute";
        }

        @Override
        public String getDescription() {
            return "Game attribute management commands";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            if (args.length < 2) {
                UtilMessage.message(player, "Game", "Usage: <alt2>/game attribute <attribute> set <value>");
                UtilMessage.message(player, "Game", "Usage: <alt2>/game attribute <attribute> get");
                UtilMessage.message(player, "Game", "Usage: <alt2>/game attribute <attribute> reset");
                return;
            }

            String attributeName = args[0];
            final GameAttribute<?> attribute = attributeManager.getAttribute(attributeName);
            if (attribute == null) {
                UtilMessage.message(player, "Game", "Unknown attribute: <alt2>" + attributeName);
                return;
            }

            switch (args[1].toLowerCase()) {
                case "set" -> setAttributeValue(player, attribute, Arrays.copyOfRange(args, 2, args.length));
                case "get" -> getAttributeValue(player, attribute);
                case "reset" -> resetAttributeValue(player, attribute);
                default -> UtilMessage.message(player, "Game", "Unknown command: <alt2>" + args[1]);
            }
        }

        @Override
        public List<String> processTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return attributeManager.getAttributes().stream()
                        .map(GameAttribute::getKey)
                        .toList();
            } else if (args.length == 2) {
                return List.of("set", "get", "reset");
            } else if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
                final GameAttribute<?> attribute = attributeManager.getAttribute(args[0]);
                if (attribute != null) {
                    return getPossibleValues(attribute);
                } else {
                    return List.of();
                }
            } else {
                return super.processTabComplete(sender, args);
            }
        }

        private <T> List<String> getPossibleValues(GameAttribute<T> attribute) {
            return attribute.getPossibleValues().stream()
                    .map(attribute::formatValue)
                    .toList();
        }

        private <T> void getAttributeValue(Player player, GameAttribute<T> attribute) {
            if (!attribute.canGet(player)) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>" + attribute.getCannotGetMessage());
                return;
            }

            final String value = attribute.formatValue(attribute.getValue());
            UtilMessage.message(player, "Game", "Attribute <alt>" + attribute.getKey() + "</alt>: <alt2>" + value);
        }

        private void resetAttributeValue(Player player, GameAttribute<?> attribute) {
            if (!attribute.canSet(player)) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>" + attribute.getCannotSetMessage());
                return;
            }

            if (attribute.resetToDefault()) {
                UtilMessage.message(player, "Game", "Attribute <alt>" + attribute.getKey() + "</alt> reset to default value");
                getAttributeValue(player, attribute);
                SoundEffect.HIGH_PITCH_PLING.play(player);
            } else {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>Failed to reset attribute <alt>" + attribute.getKey() + "</alt>");
            }
        }

        private <T> void setAttributeValue(Player player, GameAttribute<T> attribute, String[] args) {
            if (args.length == 0) {
                UtilMessage.message(player, "Game", "Usage: <alt2>/game attribute <attribute> set <value>");
                return;
            }

            if (!attribute.canSet(player)) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>" + attribute.getCannotSetMessage());
                return;
            }

            final String valueString = String.join(" ", args);
            final T value = attribute.parseValue(valueString);
            if (value == null) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>Invalid value for attribute <alt>" + attribute.getKey() + "</alt>");
                return;
            }

            if (!attribute.isValidValue(value)) {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>Invalid value for attribute <alt>" + attribute.getKey() + "</alt>");
                return;
            }

            if (attribute.setValue(value)) {
                UtilMessage.message(player, "Game", "Attribute <alt>" + attribute.getKey() + "</alt> set to <alt2>" + attribute.formatValue(attribute.getValue()));
                SoundEffect.HIGH_PITCH_PLING.play(player);
            } else {
                UtilMessage.message(player, "Game", "<dark_red>Error: <red>Failed to set attribute <alt>" + attribute.getKey() + "</alt>");
            }
        }
    }

}
