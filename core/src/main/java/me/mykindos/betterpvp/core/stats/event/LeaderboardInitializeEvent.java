package me.mykindos.betterpvp.core.stats.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.stats.Leaderboard;

@AllArgsConstructor
@Getter
public class LeaderboardInitializeEvent extends CustomEvent {

    private final Leaderboard<?, ?> leaderboard;

}
