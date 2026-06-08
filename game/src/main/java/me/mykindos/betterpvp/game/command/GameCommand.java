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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        return "game.command.game.description";
    }

    @Override
    public void execute(Player player, Client client, String[] args) {
        UtilMessage.message(player, "core.prefix.game", "game.command.usage.attribute-set", Component.text("/game attribute <attribute> set <value>", NamedTextColor.YELLOW));
        UtilMessage.message(player, "core.prefix.game", "game.command.usage.attribute-get", Component.text("/game attribute <attribute> get", NamedTextColor.YELLOW));
        UtilMessage.message(player, "core.prefix.game", "game.command.usage.attribute-reset", Component.text("/game attribute <attribute> reset", NamedTextColor.YELLOW));
        UtilMessage.message(player, "core.prefix.game", "game.command.usage.set", Component.text("/game set <game>", NamedTextColor.YELLOW));
        UtilMessage.message(player, "core.prefix.game", "game.command.usage.start", Component.text("/game start", NamedTextColor.YELLOW));
        UtilMessage.message(player, "core.prefix.game", "game.command.usage.end", Component.text("/game end", NamedTextColor.YELLOW));
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
        return "game.command.game-start.description";
    }

        @Override
        public void execute(Player player, Client client, String[] args) {
            if (serverController.getCurrentState() == GameState.IN_GAME || serverController.getCurrentState() == GameState.ENDING) {
                UtilMessage.message(player, "core.prefix.game", "game.command.start.error-in-game", Component.text("Error:", NamedTextColor.DARK_RED));
                return;
            }

            if (!transitionHandler.checkStateRequirements(true)) {
                UtilMessage.message(player, "core.prefix.game", "game.command.start.error-not-enough-players", Component.text("Error:", NamedTextColor.DARK_RED));
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
        return "game.command.game-end.description";
    }

        @Override
        public void execute(Player player, Client client, String[] args) {
            if (serverController.getCurrentState() != GameState.IN_GAME) {
                UtilMessage.message(player, "core.prefix.game", "game.command.end.error-not-in-game", Component.text("Error:", NamedTextColor.DARK_RED));
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
        return "game.command.game-set.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, "core.prefix.game", "game.command.usage.set", Component.text("/game set <game>", NamedTextColor.YELLOW));
                return;
            }

            if (serverController.getCurrentState() != GameState.WAITING) {
                UtilMessage.message(player, "core.prefix.game", "game.command.set.error-only-waiting", Component.text("Error:", NamedTextColor.DARK_RED));
                return;
            }

            String gameName = String.join(" ", args);
            final Optional<AbstractGame<?, ?>> foundOpt = gameRegistry.getRegisteredGames().stream()
                    .filter(game -> game.getConfiguration().getName().equalsIgnoreCase(gameName))
                    .findFirst();
            if (foundOpt.isEmpty()) {
                UtilMessage.message(player, "core.prefix.game", "game.command.set.error-unknown-game", Component.text("Error:", NamedTextColor.DARK_RED), Component.text(gameName, NamedTextColor.GREEN));
                return;
            }

            // get a map
            final AbstractGame<?, ?> game = foundOpt.get();
            final Optional<MappedWorld> mapOpt = mapManager.selectRandomMap(game);
            if (mapOpt.isEmpty()) {
                UtilMessage.message(player, "core.prefix.game", "game.command.set.error-no-maps", Component.text("Error:", NamedTextColor.DARK_RED), Component.text(gameName, NamedTextColor.GREEN));
                return;
            }

            if (serverController.getCurrentGame() == game) {
                UtilMessage.message(player, "core.prefix.game", "game.command.set.error-already-set", Component.text("Error:", NamedTextColor.DARK_RED), Component.text(gameName, NamedTextColor.GREEN));
                return;
            }

            // set
            serverController.setGame(game);
            mapManager.setCurrentMap(mapOpt.get());
            UtilMessage.message(player, "core.prefix.game", "game.command.set.success", Component.text(gameName, NamedTextColor.GREEN));
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
        return "game.command.game-attribute.description";
    }

        @Override
        public void execute(Player player, Client client, String[] args) {
            if (args.length < 2) {
                UtilMessage.message(player, "core.prefix.game", "game.command.usage.attribute-set", Component.text("/game attribute <attribute> set <value>", NamedTextColor.YELLOW));
                UtilMessage.message(player, "core.prefix.game", "game.command.usage.attribute-get", Component.text("/game attribute <attribute> get", NamedTextColor.YELLOW));
                UtilMessage.message(player, "core.prefix.game", "game.command.usage.attribute-reset", Component.text("/game attribute <attribute> reset", NamedTextColor.YELLOW));
                return;
            }

            String attributeName = args[0];
            final GameAttribute<?> attribute = attributeManager.getAttribute(attributeName);
            if (attribute == null) {
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.unknown-attribute", Component.text(attributeName, NamedTextColor.YELLOW));
                return;
            }

            switch (args[1].toLowerCase()) {
                case "set" -> setAttributeValue(player, attribute, Arrays.copyOfRange(args, 2, args.length));
                case "get" -> getAttributeValue(player, attribute);
                case "reset" -> resetAttributeValue(player, attribute);
                default -> UtilMessage.message(player, "core.prefix.game", "game.command.attribute.unknown-command", Component.text(args[1], NamedTextColor.YELLOW));
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
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.error-cannot-get", Component.text(attribute.getCannotGetMessage(), NamedTextColor.RED));
                return;
            }

            final String value = attribute.formatValue(attribute.getValue());
            UtilMessage.message(player, "core.prefix.game", "game.command.attribute.get-value", Component.text(attribute.getKey(), NamedTextColor.GREEN), Component.text(value, NamedTextColor.YELLOW));
        }

        private void resetAttributeValue(Player player, GameAttribute<?> attribute) {
            if (!attribute.canSet(player)) {
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.error-cannot-set", Component.text(attribute.getCannotSetMessage(), NamedTextColor.RED));
                return;
            }

            if (attribute.resetToDefault()) {
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.reset-success", Component.text(attribute.getKey(), NamedTextColor.GREEN));
                getAttributeValue(player, attribute);
                SoundEffect.HIGH_PITCH_PLING.play(player);
            } else {
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.error-reset-failed", Component.text("Error:", NamedTextColor.DARK_RED), Component.text(attribute.getKey(), NamedTextColor.GREEN));
            }
        }

        private <T> void setAttributeValue(Player player, GameAttribute<T> attribute, String[] args) {
            if (args.length == 0) {
                UtilMessage.message(player, "core.prefix.game", "game.command.usage.attribute-set", Component.text("/game attribute <attribute> set <value>", NamedTextColor.YELLOW));
                return;
            }

            if (!attribute.canSet(player)) {
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.error-cannot-set", Component.text(attribute.getCannotSetMessage(), NamedTextColor.RED));
                return;
            }

            final String valueString = String.join(" ", args);
            final T value = attribute.parseValue(valueString);
            if (value == null) {
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.error-invalid-value", Component.text("Error:", NamedTextColor.DARK_RED), Component.text(attribute.getKey(), NamedTextColor.GREEN));
                return;
            }

            if (!attribute.isValidValue(value)) {
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.error-invalid-value", Component.text("Error:", NamedTextColor.DARK_RED), Component.text(attribute.getKey(), NamedTextColor.GREEN));
                return;
            }

            if (attribute.setValue(value)) {
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.set-success", Component.text(attribute.getKey(), NamedTextColor.GREEN), Component.text(attribute.formatValue(attribute.getValue()), NamedTextColor.YELLOW));
                SoundEffect.HIGH_PITCH_PLING.play(player);
            } else {
                UtilMessage.message(player, "core.prefix.game", "game.command.attribute.error-set-failed", Component.text("Error:", NamedTextColor.DARK_RED), Component.text(attribute.getKey(), NamedTextColor.GREEN));
            }
        }
    }

}
