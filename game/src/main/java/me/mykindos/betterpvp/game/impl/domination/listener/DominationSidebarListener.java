package me.mykindos.betterpvp.game.impl.domination.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.domination.Domination;
import me.mykindos.betterpvp.game.impl.domination.controller.GameController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

@GameScoped
public class DominationSidebarListener implements Listener {

    private final Domination game;
    private final GameController controller;

    @Inject
    public DominationSidebarListener(Domination game, GameController controller) {
        this.game = game;
        this.controller = controller;
    }

    @EventHandler
    public void onBuild(SidebarBuildEvent event) {
        final SidebarComponent.Builder builder = event.getBuilder();
        builder.addBlankLine();
        builder.addDynamicLine(() -> {
            final int score = game.getConfiguration().getScoreToWinAttribute().getValue();
            return Component.text(String.format("First to %,d", score), NamedTextColor.WHITE);
        });
        builder.addBlankLine();

        // Display team scores
        game.getTeams().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(TeamProperties::name).reversed()))
                .forEach(entry -> {
                    TeamProperties properties = entry.getKey();
                    Team team = entry.getValue();
                    builder.addStaticLine(Component.text(properties.name() + " Team", properties.color()));
                    builder.addDynamicLine(() -> {
                        final int score = controller.getScores().getOrDefault(team, 0);
                        return Component.text(String.format("%,d", score), NamedTextColor.WHITE);
                    });
                    builder.addBlankLine();
                });

        // Capture Points
        controller.getCapturePoints().forEach(point -> {
            builder.addDynamicLine(() -> {
                final boolean flash = Bukkit.getCurrentTick() % 40 <= 20;
                final Style style = switch (point.getState()) {
                    case NEUTRAL ->
                            Style.style(NamedTextColor.WHITE);
                    case CAPTURED ->
                            Style.style(Objects.requireNonNull(point.getOwningTeam(), "No owning team").getProperties().color());
                    case CAPTURING -> {
                        // For CAPTURING, flash uses capturing team's color in bold; non-flash uses owning team's color (or white) unbolded.
                        final Team capturingTeam = point.getCapturingTeam();
                        final TextColor capturingColor = capturingTeam != null
                                ? capturingTeam.getProperties().color()
                                : NamedTextColor.WHITE;
                        final TextColor owningColor = point.getOwningTeam() != null
                                ? point.getOwningTeam().getProperties().color()
                                : NamedTextColor.WHITE;
                        yield flash
                                ? Style.style(capturingColor).decoration(TextDecoration.BOLD, true)
                                : Style.style(owningColor);
                    }
                    case REVERTING -> {
                        // For REVERTING, flash uses white; non-flash uses owning team's color (or white), no bold.
                        final TextColor owningColor = point.getOwningTeam() != null
                                ? point.getOwningTeam().getProperties().color()
                                : NamedTextColor.WHITE;
                        yield Style.style(owningColor);
                    }
                };
                return Component.text(point.getName(), style);
            });
        });

    }
}