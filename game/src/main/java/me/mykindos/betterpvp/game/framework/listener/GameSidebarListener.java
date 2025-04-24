package me.mykindos.betterpvp.game.framework.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.sidebar.Sidebar;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarController;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarType;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.listener.state.TransitionHandler;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.manager.RoleSelectorManager;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.MaxPlayersAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.RequiredPlayersAttribute;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class GameSidebarListener implements Listener {

    private final ServerController serverController;
    private final PlayerController playerController;
    private final ClientManager clientManager;
    private final RoleSelectorManager selectorManager;
    private final TransitionHandler transitionHandler;
    private final SidebarController sidebarController;
    private final MapManager mapManager;

    @Inject
    public GameSidebarListener(ServerController serverController, PlayerController playerController, ClientManager clientManager,
                               RoleSelectorManager selectorManager, TransitionHandler transitionHandler, SidebarController sidebarController,
                               MapManager mapManager) {
        this.serverController = serverController;
        this.playerController = playerController;
        this.clientManager = clientManager;
        this.selectorManager = selectorManager;
        this.transitionHandler = transitionHandler;
        this.sidebarController = sidebarController;
        this.mapManager = mapManager;
        setupStateHandlers();
        this.sidebarController.setDefaultProvider(this::getSidebar);
    }

    private void setupStateHandlers() {
        refreshSidebars();
        serverController.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> refreshSidebars());
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> refreshSidebars());
    }

    private void refreshSidebars() {
        for (Client client : clientManager.getOnline()) {
            final Gamer gamer = client.getGamer();
            sidebarController.resetSidebar(gamer);
        }
    }

    @EventHandler
    public void onWaitingSidebar(SidebarBuildEvent event) {
        if (serverController.getCurrentState() == GameState.IN_GAME || serverController.getCurrentState() == GameState.ENDING) {
            return;
        }

        final SidebarComponent.Builder builder = event.getBuilder();
        builder.addBlankLine();
        builder.addStaticLine(Component.text("Players", NamedTextColor.YELLOW, TextDecoration.BOLD));
        builder.addDynamicLine(() -> {
            final AbstractGame<?, ?> game = serverController.getCurrentGame();
            final int online = playerController.getParticipants().size();
            if (game == null) {
                return Component.text(online + "/0");
            }

            final int max = game.getAttribute(MaxPlayersAttribute.class).getValue();
            TextComponent text = Component.text(online + "/" + max);
            final int need = game.getAttribute(RequiredPlayersAttribute.class).getValue() - online;
            if (need > 0) {
                text = text.appendSpace().append(Component.text("(Need " + need + ")", TextColor.color(255, 158, 158)));
            }
            return text;
        });
        builder.addBlankLine();
        builder.addStaticLine(Component.text("Kit", NamedTextColor.RED, TextDecoration.BOLD));
        builder.addDynamicLine(() -> {
            return Component.text(selectorManager.getRole(event.getGamer().getPlayer()).getName());
        });
        builder.addBlankLine();
        builder.addStaticLine(Component.text("Game", NamedTextColor.GREEN, TextDecoration.BOLD));
        builder.addDynamicLine(() -> {
            final AbstractGame<?, ?> game = serverController.getCurrentGame();
            if (game == null) {
                return Component.text("None", NamedTextColor.WHITE);
            }
            return Component.text(game.getConfiguration().getName(), NamedTextColor.WHITE);
        });
        builder.addBlankLine();
        builder.addStaticLine(Component.text("Map", NamedTextColor.AQUA, TextDecoration.BOLD));
        builder.addDynamicLine(() -> {
            final MappedWorld currentMap = mapManager.getCurrentMap();
            if (currentMap == null) {
                return Component.text("None", NamedTextColor.WHITE);
            }
            return Component.text(currentMap.getMetadata().getName(), NamedTextColor.WHITE);
        });
        builder.addBlankLine();
    }

    @EventHandler
    public void onJoin(ClientJoinEvent event) {
        final Gamer gamer = event.getClient().getGamer();
        sidebarController.resetSidebar(gamer);
    }

    private Sidebar getSidebar(Gamer gamer) {
        final AbstractGame<?, ?> game = serverController.getCurrentGame();
        if (serverController.getCurrentState() != GameState.ENDING && serverController.getCurrentState() != GameState.IN_GAME) {
            final SidebarComponent waitingTitle = SidebarComponent.dynamicLine(() -> {
                if (serverController.getCurrentState() == GameState.WAITING) {
                    return Component.text("Waiting for players...", NamedTextColor.GREEN, TextDecoration.BOLD);
                } else {
                    final long estimatedTransitionTime = transitionHandler.getEstimatedTransitionTime();
                    final int remainingSeconds = (int) (Math.max(0, estimatedTransitionTime - System.currentTimeMillis()) / 1000);
                    final String qualifier = remainingSeconds == 1 ? "Second" : "Seconds";
                    return Component.text("Starting in ", NamedTextColor.WHITE, TextDecoration.BOLD)
                            .append(Component.text(remainingSeconds + " " + qualifier, NamedTextColor.GREEN, TextDecoration.BOLD));
                }
            });
            return new Sidebar(gamer, waitingTitle,  SidebarType.GENERAL);
        } else {
            return new Sidebar(gamer, game.getConfiguration().getName(), SidebarType.GENERAL);
        }
    }

}
