package me.mykindos.betterpvp.game.impl.ctf.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.core.locale.Translations;
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
            builder.addStaticLine(Translations.component("game.sidebar.ctf.team", Component.text(properties.name())).color(properties.color()).decorate(TextDecoration.BOLD));
            builder.addDynamicLine(() -> {
                final int captures = controller.getCaptures().getOrDefault(team, 0);
                return Translations.component("game.sidebar.ctf.captures", Component.text(captures)).color(NamedTextColor.WHITE);
            });
            builder.addDynamicLine(() -> {
                final Flag flag = controller.getFlag(team);
                if (flag.getState() == Flag.State.AT_BASE) {
                    return Translations.component("game.sidebar.ctf.flag-safe").color(NamedTextColor.WHITE);
                } else if (flag.getState() == Flag.State.PICKED_UP) {
                    final TextColor color = Bukkit.getCurrentTick() % 20 < 10 ? flag.getTeam().getProperties().secondary() : NamedTextColor.WHITE;
                    return Translations.component("game.sidebar.ctf.flag-taken").color(color).appendSpace().append(flag.getDisplay().getFlagCountdown());
                } else {
                    final TextColor color = Bukkit.getCurrentTick() % 20 < 10 ? flag.getTeam().getProperties().secondary() : NamedTextColor.WHITE;
                    return Translations.component("game.sidebar.ctf.flag-dropped").color(color).appendSpace().append(flag.getDisplay().getFlagCountdown());
                }
            });
            builder.addBlankLine();
        });

        builder.addStaticLine(Translations.component("game.sidebar.ctf.score-to-win").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        builder.addDynamicLine(() -> {
            final int captures = game.getConfiguration().getScoreToWinAttribute().getValue();
            return Translations.component("game.sidebar.ctf.captures", Component.text(captures)).color(NamedTextColor.WHITE);
        });
        builder.addBlankLine();

        builder.addComponent(drawable -> {
            if (controller.isSuddenDeath()) {
                drawable.drawLine(Translations.component("game.sidebar.ctf.sudden-death").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
                drawable.drawLine(Translations.component("game.sidebar.ctf.next-capture-wins").color(NamedTextColor.WHITE));
                drawable.drawLine(Translations.component("game.sidebar.ctf.no-respawns").color(NamedTextColor.WHITE));
            } else {
                drawable.drawLine(Translations.component("game.sidebar.ctf.time-left").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
                final long transitionTime = transitionHandler.getEstimatedTransitionTime() - game.getConfiguration().getSuddenDeathDurationAttribute().getValue().toMillis();
                final long remainingMillis = Math.max(0, transitionTime - System.currentTimeMillis());
                final double seconds = remainingMillis / 1000d;
                final double minutes = seconds / 60;
                if (minutes > 1) {
                    drawable.drawLine(Translations.component("game.sidebar.ctf.minutes", Component.text(String.format("%.1f", minutes))).color(NamedTextColor.WHITE));
                } else {
                    drawable.drawLine(Translations.component("game.sidebar.ctf.seconds", Component.text(String.format("%.1f", seconds))).color(NamedTextColor.WHITE));
                }
            }
        });
    }

}
