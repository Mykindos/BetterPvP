package me.mykindos.betterpvp.game.framework.model.team;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.chat.PlayerColorProvider;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamColorProvider implements PlayerColorProvider {
    @Override
    public @NotNull TextColor getColor(Player player, AbstractGame<?, ?> game) {
        Preconditions.checkArgument(game instanceof TeamGame, "Game is not a team game");
        TeamGame<?> teamGame = (TeamGame<?>) game;
        final Team team = teamGame.getPlayerTeam(player);
        if (team == null) {
            return NamedTextColor.GRAY;
        }

        return team.getProperties().color();
    }
}
