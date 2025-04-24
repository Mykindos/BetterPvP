package me.mykindos.betterpvp.game.impl.ctf.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.game.framework.listener.state.TransitionHandler;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.ctf.CaptureTheFlag;
import me.mykindos.betterpvp.game.impl.ctf.controller.GameController;
import me.mykindos.betterpvp.game.impl.ctf.model.Flag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@GameScoped
public class CTFSidebarListener implements Listener {

    private final CaptureTheFlag game;
    private final GameController controller;
    private final TransitionHandler transitionHandler;

    @Inject
    public CTFSidebarListener(CaptureTheFlag game, GameController controller, TransitionHandler transitionHandler) {
        this.game = game;
        this.controller = controller;
        this.transitionHandler = transitionHandler;
    }

    @EventHandler
    public void onBuild(SidebarBuildEvent event) {
        final SidebarComponent.Builder builder = event.getBuilder();
        builder.addBlankLine();

        game.getTeams().forEach((properties, team) -> {
            builder.addStaticLine(Component.text(properties.name() + " Team", properties.color(), TextDecoration.BOLD));
            builder.addDynamicLine(() -> {
                final int captures = controller.getCaptures().getOrDefault(team, 0);
                return Component.text(captures + " Captures", NamedTextColor.WHITE);
            });
            builder.addDynamicLine(() -> {
                final Flag flag = controller.getFlag(team);
                if (flag.getState() == Flag.State.AT_BASE) {
                    return Component.text("Flag Safe", NamedTextColor.WHITE);
                } else {
                    final TextColor color = Bukkit.getCurrentTick() % 20 < 10 ? flag.getTeam().getProperties().secondary() : NamedTextColor.WHITE;
                    return Component.text("Flag Taken", color);
                }
            });
            builder.addBlankLine();
        });

        builder.addStaticLine(Component.text("Score to Win", NamedTextColor.YELLOW, TextDecoration.BOLD));
        builder.addDynamicLine(() -> {
            final int captures = game.getConfiguration().getScoreToWinAttribute().getValue();
            return Component.text(captures + " Captures", NamedTextColor.WHITE);
        });
        builder.addBlankLine();

        builder.addComponent(drawable -> {
            if (controller.isSuddenDeath()) {
                drawable.drawLine(Component.text("Sudden Death", NamedTextColor.YELLOW, TextDecoration.BOLD));
                drawable.drawLine(Component.text("Next Capture Wins", NamedTextColor.WHITE));
                drawable.drawLine(Component.text("No Respawns", NamedTextColor.WHITE));
            } else {
                drawable.drawLine(Component.text("Time Left", NamedTextColor.YELLOW, TextDecoration.BOLD));
                final long transitionTime = transitionHandler.getEstimatedTransitionTime() - game.getConfiguration().getSuddenDeathDurationAttribute().getValue().toMillis();
                final long remainingMillis = Math.max(0, transitionTime - System.currentTimeMillis());
                final double seconds = remainingMillis / 1000d;
                final double minutes = seconds / 60;
                if (minutes > 1) {
                    drawable.drawLine(Component.text(String.format("%.1f", minutes) + " Minutes", NamedTextColor.WHITE));
                } else {
                    drawable.drawLine(Component.text(String.format("%.1f", seconds) + " Seconds", NamedTextColor.WHITE));
                }
            }
        });
    }

}
