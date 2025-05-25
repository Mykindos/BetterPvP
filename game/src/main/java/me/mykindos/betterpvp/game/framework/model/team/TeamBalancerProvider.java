package me.mykindos.betterpvp.game.framework.model.team;

import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import org.jetbrains.annotations.Nullable;

public interface TeamBalancerProvider {

    /**
     * Balances teams by moving players between teams to ensure they are evenly distributed
     * @param teamGame The team game to balance
     * @param firstBalance if this is the first time this game is being balanced (i.e. start)
     */
    void balanceTeams(TeamGame<?> teamGame, boolean firstBalance);

    /**
     * Assigns a particpant to the target team
     * @param participant
     * @param targetTeam
     * @param teamGame
     * @param playerController
     * @return
     */
    @Nullable
    Team assignToATeam(Participant participant, Team targetTeam, TeamGame<?> teamGame, PlayerController playerController);

    /**
     * Returns true if the teams are balanced,
     *
     * @param teamGame the {@link TeamGame} to check
     * @return {@code true} if teams are balanced, {@code false} otherwise
     */
    boolean isBalanced(TeamGame<?> teamGame);

}
