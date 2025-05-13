package me.mykindos.betterpvp.game.framework.model.team;

import me.mykindos.betterpvp.game.framework.TeamGame;

public interface TeamBalancerProvider {
    /**
     * Balances teams by moving players between teams to ensure they are evenly distributed
     * @param teamGame The team game to balance
     */
    void balanceTeams(TeamGame<?> teamGame);

    /**
     * Returns true if the teams are balanced,
     *
     * @param teamGame the {@link TeamGame} to check
     * @return {@code true} if teams are balanced, {@code false} otherwise
     */
    boolean isBalanced(TeamGame<?> teamGame);

}
