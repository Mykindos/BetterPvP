package me.mykindos.betterpvp.game.impl.ctf;

import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.configuration.TeamGameConfiguration;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import net.kyori.adventure.text.Component;

import java.time.Duration;

public class CaptureTheFlag extends TeamGame {

    protected CaptureTheFlag() {
        super(TeamGameConfiguration.builder()
                .teamProperty(TeamProperties.defaultBlue(6))
                .teamProperty(TeamProperties.defaultRed(6))
                .name("Capture The Flag")
                .requiredPlayers(12)
                .duration(Duration.ofMinutes(10L))
                .build());
    }

    @Override
    public Component getDescription() {
        return Component.text("Capture The Opponent's Flag").appendNewline()
                .append(Component.text("First team to 5 Captures")).appendNewline()
                .append(Component.text("Or with the most Captures after 7 minutes wins"));
    }
}
