package me.mykindos.betterpvp.game.mineplex;

import com.mineplex.studio.sdk.modules.game.GameCycle;
import com.mineplex.studio.sdk.modules.game.MineplexGame;
import org.jetbrains.annotations.NotNull;

public class ChampionsGameCycle implements GameCycle {

    private final ChampionsGame game;

    public ChampionsGameCycle(ChampionsGame game) {
        this.game = game;
    }

    @Override
    public @NotNull MineplexGame createNextGame() {
        return game;
    }

    @Override
    public boolean hasNextGame() {
        return true;
    }
}
